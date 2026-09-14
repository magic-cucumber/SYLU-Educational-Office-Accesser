package main

import (
	"fmt"
	"log"
	"os"

	"server/router"
)

func main() {
	config, extra, err := ConfigureCommandLine(os.Args[1:])
	if err != nil {
		log.Fatal(err)
	}

	engine := router.New(config, extra)

	if err := engine.Run(fmt.Sprintf(":%d", config.Port)); err != nil {
		log.Fatal(err)
	}
}
