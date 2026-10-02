# Sleepy v1.0.58

> Full data migration, system calendar export, browser-style URL editing, and 347-school coverage.

## What's New

### Full-data migration backup

`.sleepybackup` single-file export/import for all timetables, period tables, preferences, and widget configurations. From the settings page: tap "Export full migration backup" to generate a `.sleepybackup` file, tap "Import from backup" to restore it on a new device or after reinstall. Two import modes: overwrite (wipes local data first) or merge (preserves local data, only adds content from the backup). Supports periodic manual backup without relying on any cloud sync.

### System calendar export

From the export page, tap "Add to system calendar" to open a permission-aware dialog that asks for calendar read/write access only at first use. Choose target calendar, select import range (next 7 days / next month / remaining semester), preview the event list, then batch-write. The importer respects holiday makeup mappings and supports two alarm strategies: a normal reminder minutes before each class, and an experimental alarm-only trigger for first-period slots (falls back gracefully when the system does not grant alarm permission). An index tracks which calendar events were imported by Sleepy so old events can be selectively deleted when the timetable changes.

### Browser-style URL bar

The import WebView top bar now shows a live-editable URL field. Tap it to edit, tap again to navigate, or use the submit button. Long URLs scroll horizontally so the cursor can reach the end. The back arrow in the top bar exits URL edit mode first; a second press exits the import screen. URL drafts are preserved during navigation callbacks.

### Grid laboratory switches

Two independent, default-off switches in General Settings:

- **Show grid separators**: adds horizontal and vertical lines inside the course grid.
- **Long-break spacing**: detects gaps between consecutive periods (over 45 min, up to 4 h, midday or evening zones) and increases the row gap for those rows in both the timetable and WeekGrid widget.

Both are off by default and persist across restarts.

## Fixes

### Xiaomi Super Island restored

Xiaomi, Redmi, and POCO devices show course reminders in the status-bar Super Island again. On the tested Xiaomi device, the Fluid Cloud card reappears in the Super Island instead of falling back to a drawer card with a plain progress bar. The Android official live-update progress style is used again (MIUI does not promote a bare progress bar to the island), and the private Xiaomi-only extras that HyperOS rejected are gone. vivo/iQOO, Meizu, and Samsung keep their own vendor paths behind existing capability checks.

### Draft box now saves before courses are parsed

Leaving the academic-import screen via "keep as draft" now saves a school-level snapshot even when no courses have been loaded yet (before the WebView login succeeds). The snapshot records the selected school and returns to the WebView login page when restored, rather than going straight to the configuration preview. Previously, saving a draft before courses loaded silently discarded it.

### URL cursor follows long URLs

Clicking into the URL input field now places the cursor where you tap. Dragging the cursor over a long URL scrolls the input field horizontally so the selected position stays visible.

### Wisedu session expiry shows a clear message

When a Wisedu-based school returns an HTTP 403 or 401 for a stale session, the app shows a clear "session expired" message instead of an opaque JSON parsing error. This was observed on Anhui Second Medical College (ehall.ahyz.cn).

### AHYZ portal URL updated

The Anhui Second Medical College entry was switched to the portal page (`ehall.ahyz.cn/new/index.html`) that was verified to work in testing. The previous deep `wdkb/index.do` link went stale and returned 403 for the stale session.

### ZUFE student login entry corrected

The Zhejiang University of Finance and Economics entry now points to the student SSO page (`/sso/driotlogin`). The previously listed `login_slogin.html` page is reserved for teachers; students who opened it were bounced back to the login screen in a loop.

### Hangzhou City University (HZCU) support

Hangzhou City University is now covered (346 → 347 schools). The entry uses the dedicated SSO entrance (`ijw.hzcu.edu.cn/sso/ddlogin`); the generic academic-system login page lacks the app-integration callback and would loop back to the login screen. The verified schedule capture (16 courses) parses with zero parser changes. Thanks to WestGu for the detailed capture package in issue #90, and to the maintainers of shangkeschedule, Xu-Jack11/MySchedule, and LanternCX/HZCUCourseChoose for cross-repository corroboration.

### Anhui Second Medical College (ahyz) support

Full wisedu protocol adaptation for Anhui Second Medical College. Compatible with their SFSY field (indicates current semester via a boolean flag in the term API). Falls back to the first term row when the term list has a single entry and no explicit current marker.

### MJTNC chaoxing parsing fixed

Fujian Minjiang Normal College (chaoxing) fetch JS base-path detection was fixed: the portal's `/admin` pathname (without trailing slash) previously returned a bare-path 404 for all three fetch calls. Now uses a regex that matches `/admin` with or without a trailing slash, with an iframe xhid fallback for Syswin-variant deployments. The schedule parser also now merges courses with the same (name, day, teacher) by joining their period numbers and taking the union of their week sets. A single-row fallback handles cases where the server returns one course per week instead of consolidated rows.

### Widget 3-day window anchors to nearest busy day

When the widget auto-jumps to the nearest day with classes, the compact three-day window in the week-list and week-view widgets now anchors on that exact date instead of the target week's Monday. Today-first places the window starting today; today-second keeps it in the middle. Windows that cross week boundaries continue to work.

### Undo capsule no longer overlaps on narrow screens

The default 32 dp undo capsule stays unchanged. On narrow screens where the measured navigation width leaves insufficient space, the capsule shrinks to fit instead of overlapping adjacent controls.

### Table header column alignment

The timetable grid now uses a unified column width for all three header rows (weekday label / start time / end time). Elements in each row align to the midpoint of their column; rows retain their natural vertical stagger. Preview, timetable grid, and all widget variants use the same algorithm.

### Table header font scales down without truncating

Header font ranges from 11 sp to 16 sp, driven by the measured card height. The time columns widen to fit the target font size without truncating labels. The same adaptive algorithm applies to previews, the timetable, and widgets.

### Course names centered inside wrapped cards

When a course name wraps to two lines inside a course card, each line is now horizontally centered within the card instead of left-aligned. Affects both normal and conflict cards.

### Widget preview images restored

Static widget preview images are used for launcher previews. Runtime bitmap container previews that caused launcher display issues were removed.

### Widget background matches current theme

All widget variants now use the same soft accent color from the current Material theme as their background, consistent with the refresh button and other UI elements.

### Live update test button

A "Live update test" button appears below the Fluid Cloud switch when the switch is on. Tapping it runs the real Fluid Cloud service with a 2-minute window through the same notification path as actual reminders. You can check whether the OEM island or capsule appears right away instead of waiting for a class-start window.

### Notification channel routing improved

"Go to notification channel settings" now prioritizes channel-specific settings pages (confirmed working on OxygenOS). Battery optimization routes to vendor power-management pages first (Xiaomi PowerKeeper, Huawei, Honor, vivo, OPPO), with the standard Android battery optimization list as fallback.

### Reference index

- **Issues:** #58 (ZUFE student SSO entry corrected; fix cherry-picked into this release from its branch), #59 (distinct debug package identity, delivered by PR #68), and #90 (HZCU admission; capture package by WestGu).
- **Pull requests:** #63 (full-data migration backup export/import), #89 and #87 (AHYZ portal entry and Anhui Second Medical College integration), #88 (long-URL cursor scrolling), #86 (Wisedu semester detection via the SFSY field), #81 (MJTNC chaoxing timetable parsing), #80, #67, and #66 (Xiaomi Super Island restoration line), #74 (live-update test button and settings/battery routing), #84 (widget 3-day window anchored at the nearest busy day), #82 (undo capsule overlap on narrow screens), #85 (unified period-header column width), #73 (table-header font auto-sizing without truncation), #78 (centered course names inside wrapped cards), #79 (widget preview images restored, first contribution by @Cold577), #75 (widget background follows the current theme), #83 (system calendar export and grid laboratory features), and #72 (Windows test source paths, external contribution by @jim139129). Repository and maintenance line: #68 (debug package identity), #65 (miuiWidget declarations removed), #77 (machine paths removed from tests and fixtures), #69 (FAQ and LLM index refresh), #64 (tag-derived version and release workflow), #70 (main history repair), #76 (contributor branch and PR workflow docs), and #60 (commit gating and CI workflows).
- **Reference policy:** the references above come from commits merged into `main` in this range, verified against GitHub pull-request records. Changes without an explicit reference are listed by behavior and protocol only; no PR or issue number is fabricated.

## Known Limitations

- Super Island and other OEM live-card surfaces depend on vendor services, notification permission, and device qualification. AOSP emulators cannot verify OEM rendering.
- Calendar alarm-only mode is experimental; system alarm permission is requested separately from calendar permission and may not be available on all devices.
- Some vendor-specific live-card capabilities return `UNKNOWN` when the OS exposes no verifiable authorization state.
- Android 16 promoted-ongoing authorization is probed only on API 36 and later.

## Verification

- Tests: `./gradlew :app:testDebugUnitTest` — 2,421 tests, 0 failures, 0 errors.
- Lint: `./gradlew :app:lintDebug` — 489 warnings, 27 hints, 0 errors.
- Build: `./gradlew :app:assembleRelease` — successful; `versionName 1.0.58`, `versionCode 10058`.
- APK SHA-256:
  - arm64-v8a: `cc528486a55e3cb531a2744cdaf3c562fdccc4116e23496962d8e5899490c4de` (3,790,269 bytes)
  - armeabi-v7a: `6187cc4dad183f741296e073542b0468bfba8329ee708293d774015c360c0043` (3,787,577 bytes)
  - x86_64: `8391b9135e08593a7a8afd1ff5b0b0fa7001050b6554f53e306bb40a9860802b` (3,789,375 bytes)
  - universal: `100ca5323d87e9e502dc29b62242887c79e0443930bd863a26c010bde7bc76a3` (3,888,205 bytes)
- Baseline: `v1.0.57` tag (`220fdd38`) through `81d17e34`; 37 commits.

---

# Sleepy v1.0.58

> 全量迁移备份、系统日历导出、浏览器式网址栏、347 所学校覆盖。

## 新增功能

### 全量迁移备份

`.sleepybackup` 单文件导入导出，覆盖全部课表、作息表、偏好与小组件配置。设置页点"导出全量迁移配置"生成备份文件，点"从备份导入"在新设备或重装后恢复。两种导入模式：覆盖导入（清空本地数据后按备份还原）或合并导入（保留本地数据，仅追加备份内容）。支持定期手动备份，不依赖任何云同步。

### 系统日历导出

从导出页点"添加到系统日历"，弹窗在首次使用时才申请日历读写权限（仅此一次）。选目标日历和导入范围（未来 7 天 / 未来 1 个月 / 本学期剩余课程），预览事件列表，确认后批量写入。导入器尊重调休映射；支持课前普通提醒和第 1 节闹钟实验模式（系统不授权闹钟权限时自动降级）。索引记录每条日历事件由 Sleepy 导入，课表变更后可选择删除旧事件。

### 浏览器式网址栏

教务导入 WebView 顶栏现显示实时可编辑网址字段。点击编辑、再次点击导航或使用确认按钮。长网址横向滚动，光标可拖至任意位置。顶栏返回箭头优先退出网址编辑态，第二次才退出导入；编辑态隐藏次要动作以确保输入框宽度。导航回调期间保留网址草稿。

### 网格实验室开关

通用设置页新增两个独立实验室开关（默认关闭）：

- **显示网格分隔线**：在课表网格内增加横纵分隔线。
- **长课间留白**：检测连续节次间的大课间（超过 45 分钟、至多 4 小时，中段落在午间或晚间时段），在课表和 WeekGrid 小组件中增大对应行的行高。

两个开关均默认关闭，重启保持。

## 修复

### 小米超级岛恢复

小米、Redmi、POCO 的课程提醒重新进入状态栏超级岛。小米真机实测，流体云卡片重新出现在超级岛，不再降级为带进度条的普通通知卡片。恢复使用 Android 官方实时更新的进度样式（MIUI 不把裸进度条提升为岛），同时移除了被 HyperOS 拒绝的小米私有 extras。vivo/iQOO、魅族、三星继续走各自的厂商通路，受既有能力探测控制。

### 草稿箱在课程解析前即可保存

在 WebView 登录成功之前（尚未加载课程时）退出教务导入页面并选择"保留草稿"，现在会保存学校级快照，恢复时回到 WebView 登录页而非直接进入配置预览页。此前未解析阶段保存草稿会静默丢弃。

### 长网址光标跟随

点击网址输入框时光标落在点击位置。拖拽光标时光标经过的部分横向滚动，长教务链接全程可操作。

### Wisedu 会话过期提示明确

Wisedu 院校返回 HTTP 403/401（会话过期）时，页面显示"登录已过期，请在页面中重新登录后再点「导入此页」"，而非原先不透明的 JSON 解析错误。在安徽第二医学院（ehall.ahyz.cn）观察到该问题。

### 安徽第二医学院入口更新

安徽第二医学院入口改为经实测可用的门户页（`ehall.ahyz.cn/new/index.html`）。原先的深层 wdkb/index.do 链接在会话过期后返回 403。

### 浙财大学生登录入口修正

浙江财经大学条目改为学生统一认证入口（`/sso/driotlogin`）。原先指向的 `login_slogin.html` 页面仅供教师使用，学生打开会被弹回登录页形成死循环。

### 浙大城市学院（HZCU）支持

浙大城市学院现已收录（346 → 347 所）。条目使用 SSO 专用入口（`ijw.hzcu.edu.cn/sso/ddlogin`）；教务通用登录页缺少应用集成回调，会弹回登录页死循环。实测课表采集（16 门课）解析零改动。感谢 WestGu 在 issue #90 提交的详细采集包，感谢 shangkeschedule、Xu-Jack11/MySchedule、LanternCX/HZCUCourseChoose 维护者的跨仓互证。

### 安徽第二医学院（ahyz）支持

安徽第二医学院 wisedu 协议完整适配。兼容其 SFSY 字段（学期 API 中以布尔值标记当前学期）。当学期列表唯一且无显式当前标记时，安全回退取首行。

### 闽江师范超星解析修复

闽江师范（chaoxing）fetch JS 的 base-path 推断修复：门户 pathname `/admin`（无尾斜杠）此前导致三个 fetch 全部裸路径 404；改为正则匹配带或不带斜杠，并增加 iframe xhid 回退（适配 Syswin 变体部署）。解析器合并逻辑改为按（课名、星期、教师）分组，节号连续段合并为一条，周次取并集，同节多教室用 "/" 连接。修复因按周次换教室返回多行导致的重复课程卡（42 行 → 40 条，无重叠）。

### 小组件 3 日窗锚定最近有课日

小组件自动跳转到最近有课日时，week-list 和 week-view 小组件的紧凑三日窗口现在锚定该日期而非目标周周一。今日优先则窗口以今天为起点，今日居中则窗口以今天居中。跨周边界窗口继续正常工作。

### 窄屏撤回胶囊不再重叠

默认 32 dp 撤回胶囊尺寸不变。在窄屏测量导航宽度不足时，胶囊主动收窄以避免与相邻控件重叠。

### 表头列统一卡宽

课表网格三行表头（星期标签 / 开始时间 / 结束时间）整列使用统一宽度，每行三个元素各自以最宽行对应元素的中点为准，行间固有错开保留。预览、课表网格和全部小组件共用同一算法。

### 表头字号自适应不截断

表头字号随卡片高度在 11 sp 至 16 sp 连续驱动，时间列宽度按目标字号撑开而不截断标签。预览、课表和小组件使用相同自适应算法。

### 换行课程名盒内居中

课程名在卡片内换行时，各行现在水平居中，不再左对齐。影响普通课程卡和冲突课程卡。

### 小组件预览图恢复

桌面启动器预览使用静态预览图。移除了此前导致启动器显示异常的运行时位图容器预览。

### 小组件底色跟随主题

全部小组件变体现在统一使用当前 Material 主题的柔和主题色作为底色，与刷新按钮等 UI 元素一致。

### 实时更新测试按钮

流体云开关打开后，按钮下方出现"流体云测试"按钮。点击走真实流体云服务，2 分钟测试窗口，与正式提醒完全相同的通知通路。不用等课前提醒窗口，当场就能确认厂商岛或胶囊出不出来。

### 通知通道路由优化

"跳转通知通道设置"优先进入通道专属设置页（OxygenOS 实测可用）。电池优化优先跳转厂商省电页（小米、华为、荣耀、vivo、OPPO），标准 Android 电池优化列表作为备用入口。

### 引用索引

- **Issues：** #58（浙财大学生统一认证入口修正，修复自其分支 cherry-pick 进本版）、#59（debug 包独立身份，由 PR #68 落地）、#90（浙大城市学院收录，采集包来自 WestGu）。
- **Pull requests：** #63（全量迁移备份导出/导入）、#89 和 #87（AHYZ 门户入口与安徽第二医学院适配）、#88（长网址光标横向跟随）、#86（Wisedu 学期识别兼容 SFSY 字段）、#81（闽江师范超星课表解析）、#80、#67、#66（小米超级岛恢复线）、#74（流体云实测按钮与设置/电池优化跳转）、#84（小组件 3 日窗锚定最近有课日）、#82（窄屏撤回胶囊不重叠）、#85（表头列统一卡宽）、#73（表头字号自适应不截断）、#78（换行课程名盒内居中）、#79（小组件选择器预览图恢复，外部首次贡献者 @Cold577）、#75（小组件底色跟随主题）、#83（系统日历导出与网格实验室功能），以及 #72（Windows 测试源路径，外部贡献者 @jim139129）。仓库与维护线：#68（debug 包身份规范）、#65（摘除 miuiWidget 声明）、#77（清理测试与 fixture 中的机器路径）、#69（FAQ 与 LLM 索引刷新）、#64（版本 tag 派生与发布工作流）、#70（main 历史修复）、#76（贡献者分支与 PR 操作文档）、#60（提交门禁与 CI 工作流）。
- **引用原则：** 上述引用均来自本范围内合入 `main` 的提交，并对照 GitHub Pull Request 记录逐一核实。没有明确 PR/Issue 编号的改动只按实际行为和协议列出，不臆造编号。

## 已知限制

- 超级岛及其他厂商实时卡片依赖厂商系统服务、通知权限及设备资格。AOSP 模拟器无法验证厂商界面渲染。
- 日历闹钟实验模式独立于日历权限申请，部分设备可能无法授权闹钟权限。
- 系统未提供可验证授权状态时，部分厂商实时卡片能力返回 `UNKNOWN`。
- Android 16 promoted-ongoing 授权只在 API 36 及以上探测。

## 验证

- 单测：`./gradlew :app:testDebugUnitTest` — 2,421 个测试，0 失败，0 错误。
- lint：`./gradlew :app:lintDebug` — 489 条 warning、27 条 hint、0 error。
- 构建：`./gradlew :app:assembleRelease` — 成功；`versionName 1.0.58`、`versionCode 10058`。
- APK SHA-256：
  - arm64-v8a：`cc528486a55e3cb531a2744cdaf3c562fdccc4116e23496962d8e5899490c4de`（3,790,269 字节）
  - armeabi-v7a：`6187cc4dad183f741296e073542b0468bfba8329ee708293d774015c360c0043`（3,787,577 字节）
  - x86_64：`8391b9135e08593a7a8afd1ff5b0b0fa7001050b6554f53e306bb40a9860802b`（3,789,375 字节）
  - universal：`100ca5323d87e9e502dc29b62242887c79e0443930bd863a26c010bde7bc76a3`（3,888,205 字节）
- 基线：v1.0.57 tag（`220fdd38`）至 `81d17e34`；37 个提交。
