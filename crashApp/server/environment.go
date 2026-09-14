package main

import (
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/alecthomas/kong"
	kongtoml "github.com/alecthomas/kong-toml"
	"server/router"
	"server/util"
)

func ConfigureCommandLine(args []string) (router.CommandLineConfig, router.CommandLineConfigExtra, error) {
	var config router.CommandLineConfig
	parser, err := kong.New(
		&config,
		kong.Name("crash-report-server"),
		kong.Configuration(kongtoml.Loader),
	)
	if err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("create configuration parser: %w", err)
	}
	if _, err := parser.Parse(args); err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, err
	}
	if config.Port < 1 || config.Port > 65535 {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("--port must be between 1 and 65535")
	}
	if strings.TrimSpace(config.CertPath) == "" {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, errors.New("--cert-path is required")
	}
	if strings.TrimSpace(config.GiteeToken) == "" {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, errors.New("--gitee-token is required")
	}

	limit, err := util.ParseByteSize(config.MaxTransportSize)
	if err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("invalid --max-transport-size: %w", err)
	}
	privateKey, err := loadPrivateKey(config.CertPath)
	if err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("load --cert-path: %w", err)
	}
	absoluteSaveDir, err := filepath.Abs(config.SaveDir)
	if err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("resolve --save-dir: %w", err)
	}
	if err := os.MkdirAll(absoluteSaveDir, 0o750); err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("create --save-dir: %w", err)
	}
	absoluteBlacklistFile, err := filepath.Abs(config.BlacklistFile)
	if err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("resolve --black-list-file: %w", err)
	}
	blacklist, err := loadBlacklist(absoluteBlacklistFile)
	if err != nil {
		return router.CommandLineConfig{}, router.CommandLineConfigExtra{}, fmt.Errorf("load --black-list-file: %w", err)
	}

	return config, router.CommandLineConfigExtra{
		SaveDir:          absoluteSaveDir,
		BlacklistFile:    absoluteBlacklistFile,
		Blacklist:        blacklist,
		MaxTransportSize: limit,
		PrivateKey:       privateKey,
		Reports:          util.NewReportStore(1024, time.Minute),
	}, nil
}

func loadBlacklist(path string) (map[string]string, error) {
	file, err := os.OpenFile(path, os.O_RDONLY|os.O_CREATE, 0o640)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	contents, err := io.ReadAll(file)
	if err != nil {
		return nil, err
	}

	blacklist := make(map[string]string)
	for lineNumber, rawLine := range strings.Split(string(contents), "\n") {
		line := strings.TrimSpace(rawLine)
		if line == "" {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			return nil, fmt.Errorf("line %d must contain an id and a reason", lineNumber+1)
		}
		id := fields[0]
		reason := strings.TrimSpace(line[len(id):])
		if reason == "" {
			return nil, fmt.Errorf("line %d must contain a non-empty reason", lineNumber+1)
		}
		blacklist[id] = reason
	}
	return blacklist, nil
}

func loadPrivateKey(path string) (*rsa.PrivateKey, error) {
	contents, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	block, _ := pem.Decode(contents)
	if block == nil {
		return nil, errors.New("PEM block not found")
	}
	if key, err := x509.ParsePKCS1PrivateKey(block.Bytes); err == nil {
		return key, key.Validate()
	}
	parsed, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, errors.New("PEM must contain an unencrypted PKCS#1 or PKCS#8 private key")
	}
	key, ok := parsed.(*rsa.PrivateKey)
	if !ok {
		return nil, errors.New("PEM private key is not RSA")
	}
	return key, key.Validate()
}
