package router

import (
	"crypto/rsa"

	"github.com/alecthomas/kong"
	"github.com/gin-gonic/gin"
	"server/util"
)

type CommandLineConfig struct {
	ConfigFile       kong.ConfigFlag `name:"config-file" short:"c" help:"path to the TOML configuration file"`
	Port             int             `name:"port" default:"8080" help:"HTTP listen port"`
	SaveDir          string          `name:"save-dir" default:"./output" help:"directory for decrypted ZIP files"`
	CertPath         string          `name:"cert-path" help:"path to the PEM encoded RSA private key (required)"`
	GiteeToken       string          `name:"gitee-token" help:"Gitee personal access token (required)"`
	BlacklistFile    string          `name:"black-list-file" default:"./blacklist.txt" help:"path to the blacklist file"`
	MaxTransportSize string          `name:"max-transport-size" default:"5MB" help:"maximum encrypted upload size"`
	DebugMode        bool            `name:"debug-mode" help:"delay each routed request by three seconds"`
}

type CommandLineConfigExtra struct {
	SaveDir          string
	BlacklistFile    string
	Blacklist        map[string]string
	MaxTransportSize int64
	PrivateKey       *rsa.PrivateKey
	Reports          *util.ReportStore
}

func New(config CommandLineConfig, extra CommandLineConfigExtra) *gin.Engine {
	if config.DebugMode {
		gin.SetMode(gin.DebugMode)
	} else {
		gin.SetMode(gin.ReleaseMode)
	}
	engine := gin.New()
	engine.Use(gin.Logger(), gin.Recovery())
	engine.Use(injectCommandLineConfig(config))
	engine.Use(injectCommandLineConfigExtra(extra))
	if config.DebugMode {
		engine.Use(debugDelay())
	}

	engine.POST("/test", testRSA)
	engine.PUT("/report", createReportToken)
	engine.POST("/report", uploadReport)
	engine.POST("/feedback", createFeedback)
	return engine
}
