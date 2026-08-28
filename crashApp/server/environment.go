package main

import (
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type Environment struct {
	Port             uint16
	SaveDir          string
	CertPath         string
	MaxTransportSize int64
	DebugMode        bool
	PrivateKey       *rsa.PrivateKey
	Tokens           *TokenCache
}

var Env Environment

func ConfigureEnvironment(args []string) error {
	flags := flag.NewFlagSet("crash-report-server", flag.ContinueOnError)
	port := flags.Int("port", 8080, "HTTP listen port")
	saveDir := flags.String("save-dir", "./output", "directory for decrypted ZIP files")
	certPath := flags.String("cert-path", "", "path to the PEM encoded RSA private key (required)")
	maxTransportSize := flags.String("max-transport-size", "5MB", "maximum encrypted upload size")
	debugMode := flags.Bool("debug-mode", false, "delay each routed request by three seconds")
	if err := flags.Parse(args); err != nil {
		return err
	}
	if flags.NArg() != 0 {
		return fmt.Errorf("unexpected positional arguments: %s", strings.Join(flags.Args(), " "))
	}
	if *port < 1 || *port > 65535 {
		return fmt.Errorf("--port must be between 1 and 65535")
	}
	if strings.TrimSpace(*certPath) == "" {
		return errors.New("--cert-path is required")
	}

	limit, err := parseByteSize(*maxTransportSize)
	if err != nil {
		return fmt.Errorf("invalid --max-transport-size: %w", err)
	}
	privateKey, err := loadPrivateKey(*certPath)
	if err != nil {
		return fmt.Errorf("load --cert-path: %w", err)
	}
	absoluteSaveDir, err := filepath.Abs(*saveDir)
	if err != nil {
		return fmt.Errorf("resolve --save-dir: %w", err)
	}
	if err := os.MkdirAll(absoluteSaveDir, 0o750); err != nil {
		return fmt.Errorf("create --save-dir: %w", err)
	}

	Env = Environment{
		Port:             uint16(*port),
		SaveDir:          absoluteSaveDir,
		CertPath:         *certPath,
		MaxTransportSize: limit,
		DebugMode:        *debugMode,
		PrivateKey:       privateKey,
		Tokens:           NewTokenCache(defaultCacheCapacity, tokenTimeout),
	}
	return nil
}

func parseByteSize(value string) (int64, error) {
	value = strings.ToUpper(strings.TrimSpace(value))
	units := []struct {
		suffix     string
		multiplier int64
	}{
		{"GIB", 1024 * 1024 * 1024}, {"GB", 1024 * 1024 * 1024},
		{"MIB", 1024 * 1024}, {"MB", 1024 * 1024},
		{"KIB", 1024}, {"KB", 1024}, {"B", 1},
	}
	multiplier := int64(1)
	for _, unit := range units {
		if strings.HasSuffix(value, unit.suffix) {
			value = strings.TrimSpace(strings.TrimSuffix(value, unit.suffix))
			multiplier = unit.multiplier
			break
		}
	}
	number, err := strconv.ParseInt(value, 10, 64)
	if err != nil || number <= 0 || number > (1<<63-1)/multiplier {
		return 0, errors.New("must be a positive byte count such as 5242880 or 5MB")
	}
	return number * multiplier, nil
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
