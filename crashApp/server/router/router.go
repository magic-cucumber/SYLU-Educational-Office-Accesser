package router

import (
	"crypto/rsa"

	"github.com/gin-gonic/gin"
	"server/util"
)

type Dependencies struct {
	PrivateKey       *rsa.PrivateKey
	SaveDir          string
	GiteeToken       string
	Blacklist        map[string]string
	MaxTransportSize int64
	DebugMode        bool
	Reports          *util.ReportStore
}

type handlers struct {
	dependencies Dependencies
}

func New(dependencies Dependencies) *gin.Engine {
	if dependencies.DebugMode {
		gin.SetMode(gin.DebugMode)
	} else {
		gin.SetMode(gin.ReleaseMode)
	}
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
