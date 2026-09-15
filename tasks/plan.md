# Implementation Plan: WakeUp Protocol Coverage

## Overview
在当前 Sleepy 工作区基础上，新增 WakeUp 中尚未覆盖且学校数量最多的 8 个协议族，并将 WakeUp 对现有协议的不同解析形态纳入有序 fallback；保留当前用户未提交改动，不回退无关文件。

## Architecture Decisions
- 以 `JwParser`/`JwParserRegistry` 为统一解析层；每个 WakeUp 构造变体保留独立 parser 或独立 fallback 顺序。
- 显式学校 type 优先使用声明 parser；声明 parser 无课程或异常时，再按特征置信度/结果数量进入 fallback。
- 新增协议先覆盖可由 WebView 已采集 source 解析的 parser 逻辑；需要专门登录/fetch endpoint 的协议单独接入 `JwFetchProtocol`/WebView，不伪造直连数据。
- 8 个目标协议按 WakeUp 学校数排序：`kingo_new`、`jz`、`south_soft`、`login_chaoxing`、`shuwei`、`suda_post`、`cumtb`、`xju_post`。其中 `shuwei` 族需拆为 WakeUp 的不同构造变体。

## Task List

### Phase 1: Baseline and protocol contracts
- [ ] 建立本轮范围与 WakeUp→Sleepy 现状矩阵，确认 8 个目标及现有同族协议。
- [ ] 阅读现有 parser、WebView fetch、fixture 和测试，记录每个可复用入口及错误路径。
- [ ] 将 WakeUp 每个 parser 变体的关键 selector/JSON schema/作息规则转成可测试的 Kotlin contract。

### Checkpoint: Baseline
- [ ] 现有 focused parser tests pass
- [ ] 工作区原有改动保持不变

### Phase 2: New protocol slices
- [ ] 实现 `kingo_new` parser 与其 3 层回退（o00oO0o 0/12/14）。
- [ ] 实现 `jz` parser wrapper 与变体 fallback。
- [ ] 实现 `south_soft`（o00000O0 variant 10）和 `login_chaoxing` JSON parser。
- [ ] 实现 shuwei shared parser variants，并覆盖数维 JSON/HTML 两种形态。
- [ ] 实现 `suda_post`、`cumtb`、`xju_post` parser/fetch contract。
- [ ] 注册 8 个协议及学校 type，补显示名、分类、URL/HTML 检测和 WebView fetch routing。

### Checkpoint: New protocols
- [ ] 每个新增 parser 至少有真实形态 fixture 和反例 fixture
- [ ] 每个新增 type 可由 registry 路由
- [ ] focused parser tests pass

### Phase 3: Existing protocol parity and fallback
- [ ] 对 WakeUp 已有同族实现逐一比较字段优先级、周次/单双周、节次和失败判定。
- [ ] 更优逻辑更新现有 parser；不同但合法形态作为有序 fallback，不覆盖现有成功结果。
- [ ] 增加 registry attempts/diagnostics 测试，锁定 fallback 顺序和 declared type 优先级。
- [ ] 补齐 WakeUp 学校 type 与 Sleepy protocol mapping 的回归测试。

### Phase 4: Attribution and verification
- [ ] 按 SOP 记录触达的上游项目并在改代码前完成 attribution 清单；同步 license UI、locale strings 和 attribution tests。
- [ ] 补齐协议 fixture 矩阵与跨语言 invariant（若新增 WebView regex）。
- [ ] 运行 attribution、目标 parser、全部 parser、lint 和 build 验证。
- [ ] 更新 SOP 版本和本轮 docs 归档；只在用户明确要求时 commit/push。

## Risks and Mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| WakeUp 混淆类同名但构造参数不同 | High | 用 `(class, args)` 作为协议身份，独立 fixtures |
| 8 个协议含登录/抓取而非纯 parser | High | parser 与 fetch 分层，先验证 source contract，不伪造 endpoint |
| fallback 误抢其他 HTML | High | 独特 marker + confidence，保留 attempts 诊断 |
| 当前工作区有未提交改动 | High | 已从当前 HEAD 开新分支，编辑前逐文件核对，绝不清理用户改动 |
| 多校协议差异导致单一 regex 漏解析 | Medium | 每种 form 单独 fixture，正反例锁定顺序与覆盖 |

## Open Questions
- 8 个新协议中需要真实登录/fetch 的协议，若当前 WebView 没有统一采集入口，将按现有 source 回调契约先落 parser，再补 endpoint-specific fetch。
- 上游仓库检索候选数量和致谢范围需在正式跨仓检索后以实际结果为准，不手工削减候选。
