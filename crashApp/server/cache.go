package main

import (
	"container/list"
	"crypto/rand"
	"encoding/hex"
	"sync"
	"time"
)

const (
	defaultCacheCapacity = 1024
	tokenTimeout         = time.Minute
)

type uploadContext struct {
	aesKey    []byte
	expiresAt time.Time
}

type cacheEntry struct {
	token   string
	context uploadContext
}

// TokenCache stores the short-lived association between a one-time token and
// its upload context. Entries expire one minute after creation.
type TokenCache struct {
	mu       sync.Mutex
	capacity int
	ttl      time.Duration
	items    map[string]*list.Element
	lru      *list.List
	now      func() time.Time
}

func NewTokenCache(capacity int, ttl time.Duration) *TokenCache {
	if capacity <= 0 {
		capacity = defaultCacheCapacity
	}
	if ttl <= 0 {
		ttl = tokenTimeout
	}
	return &TokenCache{
		capacity: capacity,
		ttl:      ttl,
		items:    make(map[string]*list.Element, capacity),
		lru:      list.New(),
		now:      time.Now,
	}
}

func (c *TokenCache) Put(aesKey []byte) (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.removeExpiredLocked()
	for {
		var raw [16]byte
		if _, err := rand.Read(raw[:]); err != nil {
			return "", err
		}
		token := hex.EncodeToString(raw[:])
		if _, exists := c.items[token]; exists {
			continue
		}

		entry := cacheEntry{
			token: token,
			context: uploadContext{
				aesKey:    append([]byte(nil), aesKey...),
				expiresAt: c.now().Add(c.ttl),
			},
		}
		c.items[token] = c.lru.PushFront(entry)
		for c.lru.Len() > c.capacity {
			c.removeElementLocked(c.lru.Back())
		}
		return token, nil
	}
}

// Take atomically consumes a token, including when the subsequent upload is
// invalid. This preserves the one-time-token contract.
func (c *TokenCache) Take(token string) ([]byte, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()

	element, ok := c.items[token]
	if !ok {
		return nil, false
	}
	entry := element.Value.(cacheEntry)
	c.removeElementLocked(element)
	if !c.now().Before(entry.context.expiresAt) {
		return nil, false
	}
	return append([]byte(nil), entry.context.aesKey...), true
}

func (c *TokenCache) removeExpiredLocked() {
	now := c.now()
	for element := c.lru.Back(); element != nil; {
		previous := element.Prev()
		entry := element.Value.(cacheEntry)
		if !now.Before(entry.context.expiresAt) {
			c.removeElementLocked(element)
		}
		element = previous
	}
}

func (c *TokenCache) removeElementLocked(element *list.Element) {
	if element == nil {
		return
	}
	entry := element.Value.(cacheEntry)
	delete(c.items, entry.token)
	c.lru.Remove(element)
}
