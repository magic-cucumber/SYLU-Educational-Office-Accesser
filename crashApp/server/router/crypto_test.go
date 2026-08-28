package router

import (
	"archive/zip"
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"testing"
)

func TestDecryptReportMatchesClientEnvelope(t *testing.T) {
	var archive bytes.Buffer
	writer := zip.NewWriter(&archive)
	entry, err := writer.Create("summary.txt")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := entry.Write([]byte("crash")); err != nil {
		t.Fatal(err)
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}

	key := bytes.Repeat([]byte{1}, 32)
	iv := bytes.Repeat([]byte{2}, aes.BlockSize)
	plaintext := archive.Bytes()
	padding := aes.BlockSize - len(plaintext)%aes.BlockSize
	padded := append(append([]byte(nil), plaintext...), bytes.Repeat([]byte{byte(padding)}, padding)...)
	block, err := aes.NewCipher(key)
	if err != nil {
		t.Fatal(err)
	}
	cipher.NewCBCEncrypter(block, iv).CryptBlocks(padded, padded)
	encrypted := append(append([]byte(nil), iv...), padded...)

	actual, err := decryptReport(key, encrypted)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(actual, plaintext) {
		t.Fatal("decrypted report differs from original ZIP")
	}
}

func TestDecryptReportRejectsInvalidZip(t *testing.T) {
	key := bytes.Repeat([]byte{1}, 32)
	iv := make([]byte, aes.BlockSize)
	plaintext := []byte("not a zip")
	padding := aes.BlockSize - len(plaintext)%aes.BlockSize
	padded := append(append([]byte(nil), plaintext...), bytes.Repeat([]byte{byte(padding)}, padding)...)
	block, err := aes.NewCipher(key)
	if err != nil {
		t.Fatal(err)
	}
	cipher.NewCBCEncrypter(block, iv).CryptBlocks(padded, padded)
	if _, err := decryptReport(key, append(iv, padded...)); err == nil {
		t.Fatal("invalid ZIP was accepted")
	}
}
