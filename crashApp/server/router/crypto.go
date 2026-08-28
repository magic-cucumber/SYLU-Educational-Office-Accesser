package router

import (
	"archive/zip"
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/rsa"
	"encoding/base64"
	"errors"
	"fmt"
)

type encryptedPayload struct {
	First  string `json:"first"`
	Second string `json:"second"`
}

func decryptRSA(privateKey *rsa.PrivateKey, payload encryptedPayload, expectedSize int) ([]byte, error) {
	if payload.First != "payload" {
		return nil, errors.New("first must be payload")
	}
	ciphertext, err := base64.StdEncoding.DecodeString(payload.Second)
	if err != nil {
		return nil, errors.New("second is not valid Base64")
	}
	plaintext, err := rsa.DecryptPKCS1v15(rand.Reader, privateKey, ciphertext)
	if err != nil {
		return nil, errors.New("RSA decryption failed")
	}
	if len(plaintext) != expectedSize {
		return nil, fmt.Errorf("decrypted payload must be %d bytes", expectedSize)
	}
	return plaintext, nil
}

// The client cryptography library prepends a 16-byte random IV and uses
// AES/CBC/PKCS5Padding (equivalent to PKCS#7 for AES).
func decryptReport(aesKey, encrypted []byte) ([]byte, error) {
	if len(aesKey) != 32 {
		return nil, errors.New("invalid AES-256 key")
	}
	if len(encrypted) < 2*aes.BlockSize {
		return nil, errors.New("encrypted report is too short")
	}
	iv := encrypted[:aes.BlockSize]
	ciphertext := append([]byte(nil), encrypted[aes.BlockSize:]...)
	if len(ciphertext)%aes.BlockSize != 0 {
		return nil, errors.New("encrypted report is not block aligned")
	}
	block, err := aes.NewCipher(aesKey)
	if err != nil {
		return nil, err
	}
	cipher.NewCBCDecrypter(block, iv).CryptBlocks(ciphertext, ciphertext)

	padding := int(ciphertext[len(ciphertext)-1])
	if padding == 0 || padding > aes.BlockSize || padding > len(ciphertext) {
		return nil, errors.New("invalid AES-CBC padding")
	}
	for _, value := range ciphertext[len(ciphertext)-padding:] {
		if int(value) != padding {
			return nil, errors.New("invalid AES-CBC padding")
		}
	}
	plaintext := ciphertext[:len(ciphertext)-padding]
	if _, err := zip.NewReader(bytes.NewReader(plaintext), int64(len(plaintext))); err != nil {
		return nil, errors.New("decrypted report is not a valid ZIP")
	}
	return plaintext, nil
}
