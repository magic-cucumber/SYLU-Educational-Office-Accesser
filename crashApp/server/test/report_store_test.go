package test

import (
	"bytes"
	"context"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"server/util"
)

func TestReportStoreReusesActiveTokenAndCompletedResult(t *testing.T) {
	store := util.NewReportStore(8, time.Minute)
	key := bytes.Repeat([]byte{1}, 32)
	var factoryCalls atomic.Int32
	factory := func() ([]byte, error) {
		factoryCalls.Add(1)
		return key, nil
	}

	token, err := store.GetOrCreateToken("device-id", factory)
	if err != nil {
		t.Fatal(err)
	}
	reusedToken, err := store.GetOrCreateToken("device-id", func() ([]byte, error) {
		t.Fatal("key factory was called for an active device task")
		return nil, nil
	})
	if err != nil {
		t.Fatal(err)
	}
	if reusedToken != token {
		t.Fatalf("active token = %q, want %q", reusedToken, token)
	}
	if factoryCalls.Load() != 1 {
		t.Fatalf("key factory calls = %d, want 1", factoryCalls.Load())
	}

	owner, ok := store.BeginUpload(token)
	if !ok || owner.Role != util.UploadOwner {
		t.Fatal("first upload did not become the owner")
	}
	follower, ok := store.BeginUpload(token)
	if !ok || follower.Role != util.UploadFollower {
		t.Fatal("second upload did not become a follower")
	}

	waited := make(chan util.UploadResult, 1)
	go func() {
		result, completed := follower.Task.Wait(context.Background())
		if !completed {
			return
		}
		waited <- result
	}()

	result := util.UploadResult{StatusCode: 200, Body: []byte(`{"success":true}`)}
	owner.Task.Complete(result)
	select {
	case actual := <-waited:
		if actual.StatusCode != result.StatusCode || !bytes.Equal(actual.Body, result.Body) {
			t.Fatalf("waiter result = %#v, want %#v", actual, result)
		}
	case <-time.After(time.Second):
		t.Fatal("waiter did not receive the completed result")
	}

	completedClaim, ok := store.BeginUpload(token)
	if !ok || completedClaim.Role != util.UploadFollower {
		t.Fatal("completed token was not retained for duplicate upload")
	}
	actual, completed := completedClaim.Task.Wait(context.Background())
	if !completed || !bytes.Equal(actual.Body, result.Body) {
		t.Fatalf("completed token result = %#v, want %#v", actual, result)
	}

	newToken, err := store.GetOrCreateToken("device-id", factory)
	if err != nil {
		t.Fatal(err)
	}
	if newToken == token {
		t.Fatal("a completed device task was incorrectly reused for a new token request")
	}
}

func TestReportStoreConcurrentTokenRequestsShareOneToken(t *testing.T) {
	store := util.NewReportStore(128, time.Minute)
	const requestCount = 32
	var factoryCalls atomic.Int32
	start := make(chan struct{})
	tokens := make([]string, requestCount)
	errors := make([]error, requestCount)
	var group sync.WaitGroup

	for index := 0; index < requestCount; index++ {
		group.Add(1)
		go func(index int) {
			defer group.Done()
			<-start
			tokens[index], errors[index] = store.GetOrCreateToken("same-device", func() ([]byte, error) {
				factoryCalls.Add(1)
				return bytes.Repeat([]byte{2}, 32), nil
			})
		}(index)
	}
	close(start)
	group.Wait()

	for index, err := range errors {
		if err != nil {
			t.Fatalf("request %d failed: %v", index, err)
		}
		if tokens[index] != tokens[0] {
			t.Fatalf("request %d got token %q, want %q", index, tokens[index], tokens[0])
		}
	}
	if factoryCalls.Load() != 1 {
		t.Fatalf("key factory calls = %d, want 1", factoryCalls.Load())
	}
}

func TestReportStoreConcurrentUploadsHaveOneOwner(t *testing.T) {
	store := util.NewReportStore(128, time.Minute)
	token, err := store.GetOrCreateToken("same-device", func() ([]byte, error) {
		return bytes.Repeat([]byte{3}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}

	owner, ok := store.BeginUpload(token)
	if !ok || owner.Role != util.UploadOwner {
		t.Fatal("initial upload did not become the owner")
	}

	const followerCount = 32
	roles := make([]util.UploadRole, followerCount)
	var group sync.WaitGroup
	for index := 0; index < followerCount; index++ {
		group.Add(1)
		go func(index int) {
			defer group.Done()
			claim, ok := store.BeginUpload(token)
			if !ok {
				t.Errorf("follower %d could not claim the token", index)
				return
			}
			roles[index] = claim.Role
			result, completed := claim.Task.Wait(context.Background())
			if !completed || result.StatusCode != 200 {
				t.Errorf("follower %d did not receive the owner result: %#v", index, result)
			}
		}(index)
	}

	owner.Task.Complete(util.UploadResult{StatusCode: 200, Body: []byte(`{"success":true}`)})
	group.Wait()
	for index, role := range roles {
		if role != util.UploadFollower {
			t.Fatalf("request %d role = %v, want follower", index, role)
		}
	}
}

func TestReportStoreFeedbackConsumesOnlyIssuedToken(t *testing.T) {
	store := util.NewReportStore(8, time.Minute)
	key := bytes.Repeat([]byte{4}, 32)
	token, err := store.GetOrCreateToken("device-id", func() ([]byte, error) {
		return key, nil
	})
	if err != nil {
		t.Fatal(err)
	}

	actualKey, deviceID, ok := store.ConsumeFeedbackToken(token)
	if !ok || deviceID != "device-id" || !bytes.Equal(actualKey, key) {
		t.Fatal("issued token was not consumed by feedback")
	}
	if _, ok := store.BeginUpload(token); ok {
		t.Fatal("feedback-consumed token was still available for upload")
	}
}

func TestReportStoreUploadExcludesFeedback(t *testing.T) {
	store := util.NewReportStore(8, time.Minute)
	token, err := store.GetOrCreateToken("device-id", func() ([]byte, error) {
		return bytes.Repeat([]byte{14}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}

	claim, ok := store.BeginUpload(token)
	if !ok || claim.Role != util.UploadOwner {
		t.Fatal("upload did not become the owner")
	}
	if _, _, ok := store.ConsumeFeedbackToken(token); ok {
		t.Fatal("feedback consumed a token already claimed by upload")
	}
	claim.Task.Complete(util.UploadResult{StatusCode: 200})
}

func TestReportStoreExpiresIssuedToken(t *testing.T) {
	now := time.Unix(1, 0)
	store := util.NewReportStoreWithClock(8, time.Minute, func() time.Time { return now })

	first, err := store.GetOrCreateToken("device-id", func() ([]byte, error) {
		return bytes.Repeat([]byte{9}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}
	now = now.Add(time.Minute)
	if _, ok := store.BeginUpload(first); ok {
		t.Fatal("expired issued token was accepted")
	}

	second, err := store.GetOrCreateToken("device-id", func() ([]byte, error) {
		return bytes.Repeat([]byte{10}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}
	if second == first {
		t.Fatal("expired token was reused")
	}
}

func TestReportStoreKeepsUploadingTaskAvailableBeyondTokenTTL(t *testing.T) {
	now := time.Unix(1, 0)
	store := util.NewReportStoreWithClock(8, time.Minute, func() time.Time { return now })
	token, err := store.GetOrCreateToken("device-id", func() ([]byte, error) {
		return bytes.Repeat([]byte{11}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}
	owner, ok := store.BeginUpload(token)
	if !ok || owner.Role != util.UploadOwner {
		t.Fatal("upload did not become the owner")
	}
	now = now.Add(time.Minute)
	follower, ok := store.BeginUpload(token)
	if !ok || follower.Role != util.UploadFollower {
		t.Fatal("expired uploading task was not retained for its follower")
	}
	owner.Task.Complete(util.UploadResult{StatusCode: 200})
}

func TestReportStoreEvictsOldestInactiveEntry(t *testing.T) {
	store := util.NewReportStore(1, time.Minute)
	first, err := store.GetOrCreateToken("first-device", func() ([]byte, error) {
		return bytes.Repeat([]byte{12}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}
	second, err := store.GetOrCreateToken("second-device", func() ([]byte, error) {
		return bytes.Repeat([]byte{13}, 32), nil
	})
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := store.BeginUpload(first); ok {
		t.Fatal("oldest inactive token was not evicted")
	}
	if claim, ok := store.BeginUpload(second); !ok || claim.Role != util.UploadOwner {
		t.Fatal("newest token was evicted")
	}
}
