package router

import (
	"crypto/rsa"

	"github.com/gin-gonic/gin"
)

type TokenCache interface {
	Put(aesKey []byte, deviceID string) (string, error)
	Take(token string) ([]byte, string, bool)
}

type Dependencies struct {
	PrivateKey       *rsa.PrivateKey
	SaveDir          string
	GiteeToken       string
	Blacklist        map[string]string
	MaxTransportSize int64
	DebugMode        bool
	Tokens           TokenCache
}

type handlers struct {
	dependencies Dependencies
}

func New(dependencies Dependencies) *gin.Engine {
	engine := gin.New()
	engine.Use(gin.Logger(), gin.Recovery())
	if dependencies.DebugMode {
		engine.Use(debugDelay())
	}

	h := &handlers{dependencies: dependencies}
	engine.POST("/test", h.testRSA)
	engine.PUT("/report", h.createReportToken)
	engine.POST("/report", h.uploadReport)
	engine.POST("/feedback", h.createFeedback)
	return engine
}
