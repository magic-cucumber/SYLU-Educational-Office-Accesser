package util

import (
	"container/list"
	"context"
	"crypto/rand"
	"encoding/hex"
	"sync"
	"time"
)

const (
	defaultReportStoreCapacity = 1024
	defaultReportTokenTTL      = time.Minute
)

// UploadResult is the immutable HTTP result shared by the upload owner and
// requests waiting for the same upload task.
type UploadResult struct {
	StatusCode int
	Body       []byte
}

type UploadRole uint8

const (
	UploadOwner UploadRole = iota
	UploadFollower
)

// UploadTask represents one report upload associated with a token.
// Only the owner should call Complete.
type UploadTask interface {
	AESKey() []byte
	Wait(context.Context) (UploadResult, bool)
	Complete(UploadResult)
}

type UploadClaim struct {
	Task UploadTask
	Role UploadRole
}

type reportTokenState uint8

const (
	reportTokenIssued reportTokenState = iota
	reportTokenUploading
	reportTokenCompleted
)

type reportTokenEntry struct {
	deviceID  string
	aesKey    []byte
	expiresAt time.Time
	state     reportTokenState
	task      *reportUploadTask
}

type reportCacheEntry struct {
	token   string
	context reportTokenEntry
}

// ReportStore owns the in-memory token and upload-task lifecycle for the
// single server process. A token remains addressable until its TTL expires so
// duplicate POST /report requests can receive the original result.
type ReportStore struct {
	mu             sync.Mutex
	capacity       int
	ttl            time.Duration
	items          map[string]*list.Element
	activeByDevice map[string]*list.Element
	lru            *list.List
	now            func() time.Time
}

func NewReportStore(capacity int, ttl time.Duration) *ReportStore {
	return newReportStore(capacity, ttl, time.Now)
}

// NewReportStoreWithClock creates a store with an injected clock for
// deterministic tests.
func NewReportStoreWithClock(capacity int, ttl time.Duration, now func() time.Time) *ReportStore {
	if now == nil {
		now = time.Now
	}
	return newReportStore(capacity, ttl, now)
}

func newReportStore(capacity int, ttl time.Duration, now func() time.Time) *ReportStore {
	if capacity <= 0 {
		capacity = defaultReportStoreCapacity
	}
	if ttl <= 0 {
		ttl = defaultReportTokenTTL
	}
	return &ReportStore{
		capacity:       capacity,
		ttl:            ttl,
		items:          make(map[string]*list.Element, capacity),
		activeByDevice: make(map[string]*list.Element),
		lru:            list.New(),
		now:            now,
	}
}

// GetOrCreateToken atomically reuses an active device task or creates a new
// token. The key factory is called only when a new task is needed.
func (s *ReportStore) GetOrCreateToken(deviceID string, createKey func() ([]byte, error)) (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.removeExpiredLocked()
	if element, ok := s.activeByDevice[deviceID]; ok {
		entry := element.Value.(reportCacheEntry)
		s.lru.MoveToFront(element)
		return entry.token, nil
	}

	aesKey, err := createKey()
	if err != nil {
		return "", err
	}

	var raw [16]byte
	for {
		if _, err := rand.Read(raw[:]); err != nil {
			return "", err
		}
		token := hex.EncodeToString(raw[:])
		if _, exists := s.items[token]; exists {
			continue
		}

		task := &reportUploadTask{
			aesKey: append([]byte(nil), aesKey...),
			done:   make(chan struct{}),
		}
		entry := reportCacheEntry{token: token, context: reportTokenEntry{
			deviceID:  deviceID,
			aesKey:    append([]byte(nil), aesKey...),
			expiresAt: s.now().Add(s.ttl),
			state:     reportTokenIssued,
			task:      task,
		}}
		element := s.lru.PushFront(entry)
		s.items[token] = element
		s.activeByDevice[deviceID] = element
		task.onComplete = func() { s.completeTask(token, task) }
		s.evictLocked()
		return token, nil
	}
}

// BeginUpload claims the token for one owner. Concurrent or completed users
// receive the same task and must return its result instead of processing data.
func (s *ReportStore) BeginUpload(token string) (UploadClaim, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.removeExpiredLocked()
	element, ok := s.items[token]
	if !ok {
		return UploadClaim{}, false
	}
	entry := element.Value.(reportCacheEntry)
	if entry.context.state != reportTokenUploading && !s.now().Before(entry.context.expiresAt) {
		s.removeElementLocked(element)
		return UploadClaim{}, false
	}
	s.lru.MoveToFront(element)

	if entry.context.state == reportTokenIssued {
		entry.context.state = reportTokenUploading
		element.Value = entry
		return UploadClaim{Task: entry.context.task, Role: UploadOwner}, true
	}
	return UploadClaim{Task: entry.context.task, Role: UploadFollower}, true
}

// ConsumeFeedbackToken preserves the original one-time-token behavior for
// feedback. A token claimed by report upload cannot be used for feedback.
func (s *ReportStore) ConsumeFeedbackToken(token string) ([]byte, string, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.removeExpiredLocked()
	element, ok := s.items[token]
	if !ok {
		return nil, "", false
	}
	entry := element.Value.(reportCacheEntry)
	if entry.context.state != reportTokenIssued || !s.now().Before(entry.context.expiresAt) {
		return nil, "", false
	}
	s.removeElementLocked(element)
	return append([]byte(nil), entry.context.aesKey...), entry.context.deviceID, true
}

func (s *ReportStore) completeTask(token string, task *reportUploadTask) {
	s.mu.Lock()
	defer s.mu.Unlock()

	element, ok := s.items[token]
	if !ok {
		return
	}
	entry := element.Value.(reportCacheEntry)
	if entry.context.task != task {
		return
	}
	entry.context.state = reportTokenCompleted
	element.Value = entry
	if active, ok := s.activeByDevice[entry.context.deviceID]; ok && active == element {
		delete(s.activeByDevice, entry.context.deviceID)
	}
}

func (s *ReportStore) removeExpiredLocked() {
	now := s.now()
	for element := s.lru.Back(); element != nil; {
		previous := element.Prev()
		entry := element.Value.(reportCacheEntry)
		if entry.context.state != reportTokenUploading && !now.Before(entry.context.expiresAt) {
			s.removeElementLocked(element)
		}
		element = previous
	}
}

func (s *ReportStore) evictLocked() {
	for s.lru.Len() > s.capacity {
		var victim *list.Element
		for element := s.lru.Back(); element != nil; element = element.Prev() {
			entry := element.Value.(reportCacheEntry)
			if entry.context.state != reportTokenUploading {
				victim = element
				break
			}
		}
		if victim == nil {
			return
		}
		s.removeElementLocked(victim)
	}
}

func (s *ReportStore) removeElementLocked(element *list.Element) {
	if element == nil {
		return
	}
	entry := element.Value.(reportCacheEntry)
	delete(s.items, entry.token)
	if active, ok := s.activeByDevice[entry.context.deviceID]; ok && active == element {
		delete(s.activeByDevice, entry.context.deviceID)
	}
	s.lru.Remove(element)
}

type reportUploadTask struct {
	aesKey     []byte
	done       chan struct{}
	onComplete func()
	once       sync.Once
	result     UploadResult
}

func (t *reportUploadTask) AESKey() []byte {
	return append([]byte(nil), t.aesKey...)
}

func (t *reportUploadTask) Wait(ctx context.Context) (UploadResult, bool) {
	select {
	case <-t.done:
		return cloneUploadResult(t.result), true
	case <-ctx.Done():
		return UploadResult{}, false
	}
}

func (t *reportUploadTask) Complete(result UploadResult) {
	t.once.Do(func() {
		t.result = cloneUploadResult(result)
		if t.onComplete != nil {
			t.onComplete()
		}
		close(t.done)
	})
}

func cloneUploadResult(result UploadResult) UploadResult {
	result.Body = append([]byte(nil), result.Body...)
	return result
}
