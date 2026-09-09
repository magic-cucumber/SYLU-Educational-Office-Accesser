package util

import (
	"errors"
	"strconv"
	"strings"
)

func ParseByteSize(value string) (int64, error) {
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
