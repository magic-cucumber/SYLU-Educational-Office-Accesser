package router

import (
	"time"

	"github.com/gin-gonic/gin"
)

func debugDelay() gin.HandlerFunc {
	return func(context *gin.Context) {
		time.Sleep(3 * time.Second)
		context.Next()
	}
}
