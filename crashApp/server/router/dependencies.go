package router

import "github.com/gin-gonic/gin"

const (
	commandLineConfigKey      = "crash-report-server.command-line-config"
	commandLineConfigExtraKey = "crash-report-server.command-line-config-extra"
)

func injectCommandLineConfig(config CommandLineConfig) gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Set(commandLineConfigKey, config)
		context.Next()
	}
}

func injectCommandLineConfigExtra(extra CommandLineConfigExtra) gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Set(commandLineConfigExtraKey, extra)
		context.Next()
	}
}

func commandLineConfigFrom(context *gin.Context) CommandLineConfig {
	value, exists := context.Get(commandLineConfigKey)
	if !exists {
		panic("command line config was not injected")
	}
	config, ok := value.(CommandLineConfig)
	if !ok {
		panic("command line config has an invalid type")
	}
	return config
}

func commandLineConfigExtraFrom(context *gin.Context) CommandLineConfigExtra {
	value, exists := context.Get(commandLineConfigExtraKey)
	if !exists {
		panic("command line config extra was not injected")
	}
	extra, ok := value.(CommandLineConfigExtra)
	if !ok {
		panic("command line config extra has an invalid type")
	}
	return extra
}
