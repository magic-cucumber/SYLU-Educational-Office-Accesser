package router

import (
	"encoding/base64"
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
	"server/util"
)

func testRSA(context *gin.Context) {
	extra := commandLineConfigExtraFrom(context)
	var payload util.EncryptedPayload
	if err := context.ShouldBindJSON(&payload); err != nil {
		fail(context, http.StatusBadRequest, errors.New("invalid JSON payload"))
		return
	}
	plaintext, err := util.DecryptRSA(extra.PrivateKey, payload, 128)
	if err != nil {
		fail(context, http.StatusBadRequest, err)
		return
	}
	succeed(context, base64.StdEncoding.EncodeToString(plaintext))
}
