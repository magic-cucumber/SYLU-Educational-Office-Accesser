package router

import (
	"encoding/base64"
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
)

func (h *handlers) testRSA(context *gin.Context) {
	var payload encryptedPayload
	if err := context.ShouldBindJSON(&payload); err != nil {
		fail(context, http.StatusBadRequest, errors.New("invalid JSON payload"))
		return
	}
	plaintext, err := decryptRSA(h.dependencies.PrivateKey, payload, 128)
	if err != nil {
		fail(context, http.StatusBadRequest, err)
		return
	}
	succeed(context, base64.StdEncoding.EncodeToString(plaintext))
}
