package main

import (
	"fmt"
	"log"
	"os"

	"server/router"
)

func main() {
	if err := ConfigureEnvironment(os.Args[1:]); err != nil {
		log.Fatal(err)
	}

	engine := router.New(router.Dependencies{
		PrivateKey:       Env.PrivateKey,
		SaveDir:          Env.SaveDir,
		GiteeToken:       Env.GiteeToken,
		Blacklist:        Env.Blacklist,
		MaxTransportSize: Env.MaxTransportSize,
		DebugMode:        Env.DebugMode,
		Tokens:           Env.Tokens,
	})

	if err := engine.Run(fmt.Sprintf(":%d", Env.Port)); err != nil {
		log.Fatal(err)
	}
}
