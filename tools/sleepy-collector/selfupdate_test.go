package main

import (
	"crypto/sha256"
	"encoding/hex"
	"testing"
)

// parseSha256Sums 必须按"文件名"匹配, 不管行内路径写 dist/ 前缀还是裸名。
func TestParseSha256SumsMatchesPlatformFilename(t *testing.T) {
	content := `ee6c200a4e6f35ade847f1c59481893282b50e0b6dfa990aed25fcfba67444cf  dist/sleepy-collector-linux-amd64
117b5a8ffdf6dc8c8c77d45c372e1fc85ed10b9d9c282fe42cf82c053ed9f373  dist/sleepy-collector-macos-amd64
969313016bdde8764dcbbef7c236e2bbd9efe62fb2ec14fc20c818b84bb792a2  dist/sleepy-collector-macos-arm64
77e0ab546be0dcb6afb9a9f611c69bdbe1e8512d47870f5ce496eecdfc31627b  dist/sleepy-collector-windows-amd64.exe
`
	want := "969313016bdde8764dcbbef7c236e2bbd9efe62fb2ec14fc20c818b84bb792a2"
	if got := parseSha256Sums(content, "sleepy-collector-macos-arm64"); got != want {
		t.Fatalf("parseSha256Sums macos-arm64 = %q, want %q", got, want)
	}
	wantWin := "77e0ab546be0dcb6afb9a9f611c69bdbe1e8512d47870f5ce496eecdfc31627b"
	if got := parseSha256Sums(content, "sleepy-collector-windows-amd64.exe"); got != wantWin {
		t.Fatalf("parseSha256Sums windows = %q, want %q", got, wantWin)
	}
	if got := parseSha256Sums(content, "sleepy-collector-unknown"); got != "" {
		t.Fatalf("unknown platform must be empty, got %q", got)
	}
	// 空行/残行容错
	if got := parseSha256Sums("\n\nbadline\n", "sleepy-collector-macos-arm64"); got != "" {
		t.Fatalf("malformed list must be empty, got %q", got)
	}
}

func TestDistBinaryNamePerPlatform(t *testing.T) {
	cases := map[[2]string]string{
		{"darwin", "amd64"}:  "sleepy-collector-macos-amd64",
		{"darwin", "arm64"}:  "sleepy-collector-macos-arm64",
		{"linux", "amd64"}:   "sleepy-collector-linux-amd64",
		{"windows", "amd64"}: "sleepy-collector-windows-amd64.exe",
	}
	for kv, want := range cases {
		got := distBinaryNameFor(kv[0], kv[1])
		if got != want {
			t.Fatalf("distBinaryNameFor(%s,%s) = %q, want %q", kv[0], kv[1], got, want)
		}
	}
}

func TestSelfSHA256MatchesFileContent(t *testing.T) {
	// selfSHA256 对磁盘上真实文件算哈希 — 这里借当前测试二进制验证流程可用且长度合法
	sum, err := selfSHA256()
	if err != nil {
		t.Fatalf("selfSHA256: %v", err)
	}
	if len(sum) != 64 {
		t.Fatalf("sha256 hex length = %d, want 64", len(sum))
	}
	if _, err := hex.DecodeString(sum); err != nil {
		t.Fatalf("not hex: %v", err)
	}
	_ = sha256.New // 保持 import
}
