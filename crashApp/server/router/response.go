package router

import "github.com/gin-gonic/gin"

type response struct {
	Success bool    `json:"success"`
	Message *string `json:"message"`
	Data    any     `json:"data"`
}

func succeed(context *gin.Context, data any) {
	context.JSON(200, response{Success: true, Data: data})
}

func fail(context *gin.Context, status int, err error) {
	message := err.Error()
	context.JSON(status, response{Success: false, Message: &message})
}
