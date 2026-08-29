package main

import (
	"bytes"
	"testing"
	"time"
)

func TestTokenCacheIsOneTimeAndCopiesKey(t *testing.T) {
	cache := NewTokenCache(2, time.Minute)
	key := bytes.Repeat([]byte{1}, 32)
	token, err := cache.Put(key, "device-id")
	if err != nil {
		t.Fatal(err)
	}
	key[0] = 2

	stored, deviceID, ok := cache.Take(token)
	if !ok || stored[0] != 1 || deviceID != "device-id" {
		t.Fatal("cache did not preserve the upload context")
	}
	if _, _, ok := cache.Take(token); ok {
		t.Fatal("token was reusable")
	}
}

func TestTokenCacheExpiresEntries(t *testing.T) {
	cache := NewTokenCache(2, time.Minute)
	now := time.Unix(1, 0)
	cache.now = func() time.Time { return now }
	token, err := cache.Put(make([]byte, 32), "")
	if err != nil {
		t.Fatal(err)
	}
	now = now.Add(time.Minute)
	if _, _, ok := cache.Take(token); ok {
		t.Fatal("expired token was accepted")
	}
}

func TestTokenCacheEvictsLeastRecentEntry(t *testing.T) {
	cache := NewTokenCache(1, time.Minute)
	first, err := cache.Put(make([]byte, 32), "")
	if err != nil {
		t.Fatal(err)
	}
	second, err := cache.Put(bytes.Repeat([]byte{1}, 32), "")
	if err != nil {
		t.Fatal(err)
	}
	if _, _, ok := cache.Take(first); ok {
		t.Fatal("least recent token was not evicted")
	}
	if _, _, ok := cache.Take(second); !ok {
		t.Fatal("newest token was evicted")
	}
}
