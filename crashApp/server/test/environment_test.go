package test

import (
	"testing"

	"server/util"
)

func TestParseByteSize(t *testing.T) {
	tests := map[string]int64{
		"1":     1,
		"1KB":   1024,
		"5MB":   5 * 1024 * 1024,
		"2 GiB": 2 * 1024 * 1024 * 1024,
	}
	for input, expected := range tests {
		actual, err := util.ParseByteSize(input)
		if err != nil {
			t.Fatalf("ParseByteSize(%q): %v", input, err)
		}
		if actual != expected {
			t.Fatalf("ParseByteSize(%q) = %d, want %d", input, actual, expected)
		}
	}
	for _, input := range []string{"", "0", "-1", "5.5MB", "MB"} {
		if _, err := util.ParseByteSize(input); err == nil {
			t.Fatalf("ParseByteSize(%q) unexpectedly succeeded", input)
		}
	}
}
