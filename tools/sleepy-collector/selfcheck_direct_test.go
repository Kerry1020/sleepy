package main

import (
	"testing"
)

// 端到端: go test 的临时二进制 SHA 必然 ≠ 线上 dist SHA (每次重编都变),
// selfCheckAndUpdate 会走"下载替换自身"分支。断言语义: ok=false + updated=true。
// 替换对象是 go test 缓存的临时二进制, 不影响任何真实产物。
func TestSelfCheckReplacesStaleSelf(t *testing.T) {
	if testing.Short() {
		t.Skip("network")
	}
	ok, updated, err := selfCheckAndUpdate()
	if err != nil {
		t.Fatalf("selfCheckAndUpdate err: %v", err)
	}
	if ok || !updated {
		t.Fatalf("stale self should yield ok=false updated=true, got ok=%v updated=%v", ok, updated)
	}
}
