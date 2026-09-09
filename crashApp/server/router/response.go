package router

import (
	"encoding/json"
	"net/http"

	"github.com/gin-gonic/gin"
	"server/util"
)

type response struct {
	Success bool    `json:"success"`
	Message *string `json:"message"`
	Data    any     `json:"data"`
}

func succeed(context *gin.Context, data any) {
	writeResult(context, successResult(data))
}

func fail(context *gin.Context, status int, err error) {
	writeResult(context, failureResult(status, err))
}

func successResult(data any) util.UploadResult {
	return responseResult(http.StatusOK, response{Success: true, Data: data})
}

func failureResult(status int, err error) util.UploadResult {
	message := err.Error()
	return responseResult(status, response{Success: false, Message: &message})
}

func responseResult(status int, payload response) util.UploadResult {
	body, err := json.Marshal(payload)
	if err != nil {
		fallback := response{Success: false}
		body, _ = json.Marshal(fallback)
		status = http.StatusInternalServerError
	}
	return util.UploadResult{StatusCode: status, Body: body}
}

func writeResult(context *gin.Context, result util.UploadResult) {
	context.Data(result.StatusCode, "application/json; charset=utf-8", result.Body)
}
