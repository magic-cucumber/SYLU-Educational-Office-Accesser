package test

import (
	"archive/zip"
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/rsa"
	"encoding/base64"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"sync"
	"testing"
	"time"

	"server/router"
	"server/util"
)

func TestReportEndpointsConcurrentTokenRequestsReuseToken(t *testing.T) {
	privateKey, err := rsa.GenerateKey(rand.Reader, 1024)
	if err != nil {
		t.Fatal(err)
	}
	aesKey := bytes.Repeat([]byte{5}, 32)
	ciphertext, err := rsa.EncryptPKCS1v15(rand.Reader, &privateKey.PublicKey, aesKey)
	if err != nil {
		t.Fatal(err)
	}
	payload, err := json.Marshal(map[string]string{
		"cipher":   base64.StdEncoding.EncodeToString(ciphertext),
		"deviceId": "same-device",
	})
	if err != nil {
		t.Fatal(err)
	}

	engine := router.New(router.Dependencies{
		PrivateKey:       privateKey,
		Blacklist:        map[string]string{},
		MaxTransportSize: 1 << 20,
		Reports:          util.NewReportStore(128, time.Minute),
	})
	const requestCount = 16
	responses := make([]*httptest.ResponseRecorder, requestCount)
	var group sync.WaitGroup
	for index := 0; index < requestCount; index++ {
		group.Add(1)
		go func(index int) {
			defer group.Done()
			request := httptest.NewRequest(http.MethodPut, "/report", bytes.NewReader(payload))
			request.Header.Set("Content-Type", "application/json")
			response := httptest.NewRecorder()
			engine.ServeHTTP(response, request)
			responses[index] = response
		}(index)
	}
	group.Wait()

	var firstToken string
	for index, response := range responses {
		if response.Code != http.StatusOK {
			t.Fatalf("request %d status = %d, body = %s", index, response.Code, response.Body.String())
		}
		var body struct {
			Success bool   `json:"success"`
			Data    string `json:"data"`
		}
		if err := json.Unmarshal(response.Body.Bytes(), &body); err != nil {
			t.Fatalf("decode response %d: %v", index, err)
		}
		if !body.Success || body.Data == "" {
			t.Fatalf("request %d returned %#v", index, body)
		}
		if index == 0 {
			firstToken = body.Data
		} else if body.Data != firstToken {
			t.Fatalf("request %d token = %q, want %q", index, body.Data, firstToken)
		}
	}

	invalidPayload := []byte(`{"cipher":"not-base64","deviceId":"new-device"}`)
	invalidRequest := httptest.NewRequest(http.MethodPut, "/report", bytes.NewReader(invalidPayload))
	invalidRequest.Header.Set("Content-Type", "application/json")
	invalidResponse := httptest.NewRecorder()
	engine.ServeHTTP(invalidResponse, invalidRequest)
	if invalidResponse.Code != http.StatusBadRequest {
		t.Fatalf("invalid new token request status = %d, body = %s", invalidResponse.Code, invalidResponse.Body.String())
	}

	existingPayload := []byte(`{"cipher":"not-base64","deviceId":"same-device"}`)
	existingRequest := httptest.NewRequest(http.MethodPut, "/report", bytes.NewReader(existingPayload))
	existingRequest.Header.Set("Content-Type", "application/json")
	existingResponse := httptest.NewRecorder()
	engine.ServeHTTP(existingResponse, existingRequest)
	if existingResponse.Code != http.StatusOK {
		t.Fatalf("invalid existing token request status = %d, body = %s", existingResponse.Code, existingResponse.Body.String())
	}
	var existingBody struct {
		Success bool   `json:"success"`
		Data    string `json:"data"`
	}
	if err := json.Unmarshal(existingResponse.Body.Bytes(), &existingBody); err != nil {
		t.Fatal(err)
	}
	if !existingBody.Success || existingBody.Data != firstToken {
		t.Fatalf("existing task response = %#v, want token %q", existingBody, firstToken)
	}
}

func TestReportEndpointDuplicateUploadReturnsFirstResultWithoutUsingNewData(t *testing.T) {
	store := util.NewReportStore(8, time.Minute)
	key := bytes.Repeat([]byte{6}, 32)
	token, err := store.GetOrCreateToken("same-device", func() ([]byte, error) {
		return key, nil
	})
	if err != nil {
		t.Fatal(err)
	}

	saveDir := t.TempDir()
	engine := router.New(router.Dependencies{
		MaxTransportSize: 1 << 20,
		SaveDir:          saveDir,
		Reports:          store,
	})
	firstReport := encryptTestZip(t, key, []byte("first report"))
	firstRequest := newUploadRequest(t, token, "11111111-1111-4111-8111-111111111111.bin", firstReport)
	firstResponse := httptest.NewRecorder()
	engine.ServeHTTP(firstResponse, firstRequest)
	assertSuccessfulUpload(t, firstResponse)

	secondRequest := newUploadRequest(t, token, "22222222-2222-4222-8222-222222222222.bin", []byte("not encrypted report"))
	secondResponse := httptest.NewRecorder()
	engine.ServeHTTP(secondResponse, secondRequest)
	assertSuccessfulUpload(t, secondResponse)

	entries, err := os.ReadDir(saveDir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 1 || entries[0].Name() != "11111111-1111-4111-8111-111111111111.zip" {
		t.Fatalf("saved reports = %#v, want only the first report", entries)
	}
}

func TestReportEndpointDuplicateUploadReceivesFirstFailure(t *testing.T) {
	store := util.NewReportStore(8, time.Minute)
	key := bytes.Repeat([]byte{7}, 32)
	token, err := store.GetOrCreateToken("same-device", func() ([]byte, error) {
		return key, nil
	})
	if err != nil {
		t.Fatal(err)
	}

	engine := router.New(router.Dependencies{
		MaxTransportSize: 1 << 20,
		SaveDir:          t.TempDir(),
		Reports:          store,
	})
	firstRequest := newUploadRequest(t, token, "33333333-3333-4333-8333-333333333333.bin", []byte("invalid"))
	firstResponse := httptest.NewRecorder()
	engine.ServeHTTP(firstResponse, firstRequest)
	if firstResponse.Code != http.StatusBadRequest {
		t.Fatalf("first status = %d, body = %s", firstResponse.Code, firstResponse.Body.String())
	}

	secondRequest := newUploadRequest(t, token, "44444444-4444-4444-8444-444444444444.bin", encryptTestZip(t, key, []byte("new report")))
	secondResponse := httptest.NewRecorder()
	engine.ServeHTTP(secondResponse, secondRequest)
	if secondResponse.Code != firstResponse.Code || secondResponse.Body.String() != firstResponse.Body.String() {
		t.Fatalf("duplicate failure = (%d, %s), want (%d, %s)", secondResponse.Code, secondResponse.Body.String(), firstResponse.Code, firstResponse.Body.String())
	}
}

func TestReportEndpointConcurrentUploadsSaveOneReport(t *testing.T) {
	store := util.NewReportStore(8, time.Minute)
	key := bytes.Repeat([]byte{8}, 32)
	token, err := store.GetOrCreateToken("same-device", func() ([]byte, error) {
		return key, nil
	})
	if err != nil {
		t.Fatal(err)
	}

	saveDir := t.TempDir()
	engine := router.New(router.Dependencies{
		MaxTransportSize: 1 << 20,
		SaveDir:          saveDir,
		Reports:          store,
	})
	requests := []struct {
		name string
		data []byte
	}{
		{"55555555-5555-4555-8555-555555555555.bin", encryptTestZip(t, key, []byte("first concurrent report"))},
		{"66666666-6666-4666-8666-666666666666.bin", encryptTestZip(t, key, []byte("second concurrent report"))},
	}
	responses := make([]*httptest.ResponseRecorder, len(requests))
	var group sync.WaitGroup
	for index, requestData := range requests {
		group.Add(1)
		go func(index int, requestData struct {
			name string
			data []byte
		}) {
			defer group.Done()
			request := newUploadRequest(t, token, requestData.name, requestData.data)
			response := httptest.NewRecorder()
			engine.ServeHTTP(response, request)
			responses[index] = response
		}(index, requestData)
	}
	group.Wait()
	for index, response := range responses {
		assertSuccessfulUpload(t, response)
		if response.Code != http.StatusOK {
			t.Fatalf("concurrent request %d status = %d", index, response.Code)
		}
	}

	entries, err := os.ReadDir(saveDir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 1 {
		t.Fatalf("saved report count = %d, want 1", len(entries))
	}
}

func newUploadRequest(t *testing.T, token, filename string, contents []byte) *http.Request {
	t.Helper()
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	if err := writer.WriteField("token", token); err != nil {
		t.Fatal(err)
	}
	file, err := writer.CreateFormFile("file", filename)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := file.Write(contents); err != nil {
		t.Fatal(err)
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	request := httptest.NewRequest(http.MethodPost, "/report", &body)
	request.Header.Set("Content-Type", writer.FormDataContentType())
	return request
}

func encryptTestZip(t *testing.T, key, content []byte) []byte {
	t.Helper()
	var archive bytes.Buffer
	writer := zip.NewWriter(&archive)
	entry, err := writer.Create("summary.txt")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := entry.Write(content); err != nil {
		t.Fatal(err)
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}

	padding := aes.BlockSize - archive.Len()%aes.BlockSize
	padded := append(append([]byte(nil), archive.Bytes()...), bytes.Repeat([]byte{byte(padding)}, padding)...)
	iv := bytes.Repeat([]byte{9}, aes.BlockSize)
	block, err := aes.NewCipher(key)
	if err != nil {
		t.Fatal(err)
	}
	cipher.NewCBCEncrypter(block, iv).CryptBlocks(padded, padded)
	return append(append([]byte(nil), iv...), padded...)
}

func assertSuccessfulUpload(t *testing.T, response *httptest.ResponseRecorder) {
	t.Helper()
	if response.Code != http.StatusOK {
		t.Fatalf("upload status = %d, body = %s", response.Code, response.Body.String())
	}
	var body struct {
		Success bool `json:"success"`
	}
	if err := json.Unmarshal(response.Body.Bytes(), &body); err != nil {
		t.Fatal(err)
	}
	if !body.Success {
		t.Fatalf("upload response = %s", response.Body.String())
	}
}
