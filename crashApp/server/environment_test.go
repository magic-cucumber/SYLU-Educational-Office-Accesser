package main

import "testing"

func TestParseByteSize(t *testing.T) {
	tests := map[string]int64{
		"1":     1,
		"1KB":   1024,
		"5MB":   5 * 1024 * 1024,
		"2 GiB": 2 * 1024 * 1024 * 1024,
	}
	for input, expected := range tests {
		actual, err := parseByteSize(input)
		if err != nil {
			t.Fatalf("parseByteSize(%q): %v", input, err)
		}
		if actual != expected {
			t.Fatalf("parseByteSize(%q) = %d, want %d", input, actual, expected)
		}
	}
	for _, input := range []string{"", "0", "-1", "5.5MB", "MB"} {
		if _, err := parseByteSize(input); err == nil {
			t.Fatalf("parseByteSize(%q) unexpectedly succeeded", input)
		}
	}
}
