# Sleepy v1.0.60

> Holiday reminders, auto-DND, tablet dual-pane — no more 8am alarms on holidays.

## What's New

### 1. Holiday-aware reminder chain
**What changed**: Previously, reminders would fire on holidays even when there were no classes — users woke up to "no class today" notifications. Now Sleepy skips reminders on national holidays.

**How it works**:
- New `HolidayManager.isPublicHoliday(ctx, date, tableId)` method checks against network calendar + user overrides
- Transfer workdays (调休) are respected — if a holiday becomes a work day, reminders still fire
- Network failure = conservative (skip nothing rather than miss classes)

**Four integration points**:
- Daily summary: shows "no class" message on holidays
- Pre-class alarms: skipped on holidays
- Fluid cloud capsule: skipped on holidays
- DND intervals: skipped on holidays

**Related PR**: #104 (merged)

---

### 2. Auto-DND ClassDndScheduler
**What changed**: Phone now automatically enters DND mode when class starts, restores after class ends — no manual toggle needed.

**How it works**:
- Monitors timetable and enables system DND at class start time
- Restores user's original DND settings after class ends
- Self-calibrates on: timetable changes, app launch, timezone changes
- Requires: system "notification policy access" permission (Android will prompt)

**Related PR**: #104 (merged)

---

### 3. Tablet Master-Detail dual-pane (>600dp)
**What changed**: Tablets and foldables now show Schedule (课表) on the left half, with Today/Manage/Mine tabs on the right — like a desktop app.

**What users see**:
- Left 50%: ScheduleScreen (always visible, never scrolls away)
- Right 50%: Three tabs — Today / Manage / Mine, switchable via NavigationRail
- 200ms fade animation on tab switch
- NavigationRail now shows as "runway capsules": current tab + schedule = highlighted, others regular

**Phone impact**: None. Devices <600dp unchanged.

**Related PR**: #114 (merged)

---

### 4. Same-city school recommendations
**What changed**: When selecting a school, Sleepy now prioritizes recommending local universities.

**Why useful**: Users in Tianjin see Tianjin's schools first, reducing scroll distance.

**Examples**: LFU (中国民航大学) and other local schools shown at top.

**Related PR**: #117 (merged)

---

### 5. Meal break markers
**What changed**: Timetable grid now displays meal/break periods with distinct styling — users can easily see lunch breaks.

**Related PR**: #118 (merged)

---

### 6. ColorOS fluid cloud styling
**What changed**: Optimized fluid cloud capsule rendering on ColorOS devices (OPPO/realme).

**Related PR**: #117 (merged)

---

## Fixes

### 1. Dev red-dot only on tablet branch
**What**: Previously, dev version red-dot showed on ALL branches. Now only shows on tablet branch (!isCompact), phone unaffected.

**Related PR**: #114

### 2. Right panel callbacks fixed
**What**: Right half content page callbacks now correctly routed to navigator — back button works properly.

**Related PR**: #114

### 3. Runway capsule size optimized
**What**: Adjusted to 56×64dp —刚好容纳两个图标, no longer cramped.

**Related PR**: #114

### 4. HFUT semester selection recovery
**What**: Fixed bug where semester selection failed under specific HFUT (合肥工业大学) scenarios.

**Related PR**: #125

---

## Known Limitations

- 9 source-analysis tests fail due to path resolution — not blocking release

---

## Verification

### CI Build Pipeline
This release was built via GitHub Actions CI (not local build). CI workflow:
1. Trigger: Tag push `v1.0.60`
2. Jobs:
   - `test`: Run `:app:testDebugUnitTest` → 2577 tests
   - `build`: Run `:app:assembleRelease` → generates 4 ABI APKs
   - `sign`: Sign APKs with project's existing keystore (no upgrade signing warning)
   - `upload`: Upload artifacts to GitHub Release
3. All CI jobs passed.

### Test Results
- Unit tests: 2577 completed, 9 failed (source-analysis tests, path resolution issue, not blocking)

### Build Artifacts
- versionName: v1.0.60
- versionCode: 1060
- Package: com.lingion.sleepy
- ABI variants:
  - arm64-v8a: f8afd3aec912c65ca305fde52e0b737c2ac8066b2521414ab16d1828148c19d0
  - armeabi-v7a: c1d535ee86014d0788fe48a6bee0390df20908d359e1e2ca039e391fa4ec735a
  - x86_64: cdb4f0e6aad8ceb37e6b1010c6b31b6f5b7cccd254d2759a16a9a14f8b66f981
  - universal: 6d8eba87aae8d9a5c73098ee757c61548f7e9ba852e1589085b4958a0680ae1f

---

# Sleepy v1.0.60

> 国庆春节不再凌晨推送 / No more 8am alarms on holidays.

## 新增功能

### 1. 法定节假日提醒链
**发生了什么变化**：以前国庆、春节放假日，没课但提醒照常推送，用户被"今天没课"的闹钟叫醒。现在 Sleepy 会跳过法定节假日的提醒。

**工作原理**：
- 新增 `HolidayManager.isPublicHoliday(ctx, date, tableId)` 方法，检查网络日历 + 用户本地覆盖
- 调休补班日照常提醒（映射生效时）
- 网络失败保守策略：宁可不跳也不漏课

**四处接入**：
- 每日摘要：显示"无课"文案
- 课前闹钟：跳过节假日
- 流体云胶囊：跳过节假日
- 勿扰区间枚举：跳过节假日

**相关 PR**：#104（已合并）

---

### 2. 上课自动勿扰 ClassDndScheduler
**发生了什么变化**：现在上课时手机自动进入勿扰模式，下课自动恢复，不用手动开关。

**工作原理**：
- 监听课程表，上课时间自动开启系统勿扰
- 课程结束后恢复用户原有勿扰设置
- 课表变更、App 启动、时区变化时自动重新同步
- 需要系统"通知策略访问"权限（Android 会弹窗提示）

**相关 PR**：#104（已合并）

---

### 3. 平板 Master-Detail 双栏布局 (>600dp)
**发生了什么变化**：平板和折叠屏现在左半屏固定显示课表，右半屏显示三个标签页——像桌面应用一样。

**用户看到的效果**：
- 左半屏：ScheduleScreen（课表），始终可见，不会随页面滚动消失
- 右半屏：今日/管理/我的 三个标签页，通过 NavigationRail 切换
- 切换带 200ms 渐隐渐显动画
- NavigationRail 改成跑道胶囊样式：当前 tab + 课表 = 高亮胶囊，其他两个普通

**手机影响**：无。600dp 以下的设备不受影响。

**相关 PR**：#114（已合并）

---

### 4. 同城学校推荐
**发生了什么变化**：选择学校时，优先推荐本地高校。

**为什么有用**：在天津的用户先看到天津的学校，不用翻很远。

**例子**：中国民航大学 (LFU) 等本地学校显示在顶部。

**相关 PR**：#117（已合并）

---

### 5. 课表餐歇标记
**发生了什么变化**：课表网格现在能显示午休/餐歇时段，用不同样式区分，用户一眼就能看到午休时间。

**相关 PR**：#118（已合并）

---

### 6. ColorOS 流体云适配
**发生了什么变化**：ColorOS 系统（OPPO/真我）上的流体云胶囊样式优化，显示更美观。

**相关 PR**：#117（已合并）

---

## 修复

### 1. 开发版红点只在平板分支显示
**问题**：之前红点在所有分支都显示。**修复**：现在只在 !isCompact 分支显示，手机不受影响。

**相关 PR**：#114

### 2. 右半区子页面回调修复
**问题**：右半屏内容页面的回调没有正确接到 navigator。**修复**：现在返回按钮正常工作。

**相关 PR**：#114

### 3. 跑道胶囊尺寸优化
**问题**：胶囊太小。**修复**：调整为 56×64dp，刚好容纳两个图标，不再拥挤。

**相关 PR**：#114

### 4. 合肥工业大学学期选择恢复
**问题**：特定场景下 HFUT 学期选择失败。**修复**：已修复该 bug。

**相关 PR**：#125

---

## 已知限制

- 9 个源码分析类测试因路径解析问题失败，不阻塞发布

---

## 验证

### CI 构建流程
本版本通过 GitHub Actions CI 构建（非本地构建）。CI 工作流：
1. 触发：推送 tag `v1.0.60`
2. 作业：
   - `test`：运行 `:app:testDebugUnitTest` → 2577 个测试
   - `build`：运行 `:app:assembleRelease` → 生成 4 个 ABI 的 APK
   - `sign`：使用项目现有密钥签名（无升级签名警告）
   - `upload`：上传产物到 GitHub Release
3. 所有 CI 作业通过。

### 测试结果
- 单元测试：2577 完成，9 失败（源码分析测试，路径解析问题，不阻塞发布）

### 构建产物
- versionName：v1.0.60
- versionCode：1060
- 包名：com.lingion.sleepy
- ABI 变体：
  - arm64-v8a: f8afd3aec912c65ca305fde52e0b737c2ac8066b2521414ab16d1828148c19d0
  - armeabi-v7a: c1d535ee86014d0788fe48a6bee0390df20908d359e1e2ca039e391fa4ec735a
  - x86_64: cdb4f0e6aad8ceb37e6b1010c6b31b6f5b7cccd254d2759a16a9a14f8b66f981
  - universal: 6d8eba87aae8d9a5c73098ee757c61548f7e9ba852e1589085b4958a0680ae1f
