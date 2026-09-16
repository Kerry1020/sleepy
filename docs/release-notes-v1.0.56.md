# Sleepy v1.0.56

## What's New

- **Per-timetable period tables** — each timetable can carry its own bell schedule: create, bind, and edit period tables independently; grid rendering, notifications, and home-screen widgets all follow the bound table.
- **Import drafts** — when an import hits conflicts you can stash it as a draft and finish it later from the drafts entry, instead of resolving everything on the spot.
- **Pinch-to-zoom rows, made permanent** — two-finger pinch on the timetable now shows a ✓ and an ↺ in the top bar: ✓ keeps the new row height across launches, ↺ reverts to the last confirmed size.
- **Lab section in Settings** (all off by default):
  - Adaptive timetable height — the grid fills the available space instead of fixed rows.
  - Auto-hide evening courses — with a user-picked evening start time.
- **Widget 3-day window** — the smallest widget size can show a rolling three-day window (today first or second), spanning into last/next week.
- **Course group colors back in place** — group color editing lives again in the course basics card, with a clear "Use custom color" switch; per-course color rows in the edit list are preserved.
- **New schools**: Southwest Jiaotong University timetable, Guangxi Vocational & Technical University of Agriculture; plus eight additional WakeUp-compatible alarm protocol families.
- **QQ group entry** on the About page — tap to copy the group number and open QQ.
- **Custom URL import slot** at the top of the school list — paste your school's academic-system address even if it is not in the directory yet.
- **Dialog buttons redesigned app-wide** — tonal block buttons with clear boundaries; labels stay on one row when they fit and stack full-width when they do not, so text is never truncated.
- **Placeholder rows for irregular-time courses** — narrow placeholder rows (e.g. a 5-minute overflow) render as subtle gray blocks; tap to expand exactly to fit the time text, tap again to collapse. Longer gaps keep showing their time range.

## Improved & Fixed

- Hefei University of Technology (EAMS5) course collection now completes all four stages.
- Returning to the timetable no longer flickers between weeks.
- Cancelling a new period table no longer leaves an empty placeholder row behind.
- Draft recovery keeps its full import configuration; draft entry points are fully wired.
- Several partner-protocol parsing fixes verified across repositories (day offsets, rowspan handling, unknown-course fallback).
- Course group color and per-course colors survive edits without clobbering each other.
- QQ group join now opens the QQ group profile card directly (previously the deep link could fail silently); falls back gracefully when QQ is not installed.
- Period-table preview now lists every affected course with its exact time change when you edit a shared period, instead of reporting "0 courses changed".

## Credits

This release cross-verified against 43 open-source university-scraper repositories (11 for the Southwest Jiaotong University timetable, 32 for the WakeUp alarm protocol family). Many protocol details were confirmed against their implementations — thank you.

---

# Sleepy v1.0.56

## 新增

- **每张课表独立节次表** —— 课表可各自绑定独立作息节次:创建、绑定、编辑互不干扰;网格渲染、通知、桌面卡片统一按绑定的节次表走。
- **导入草稿箱** —— 导入撞冲突时可先存草稿,稍后从草稿箱入口继续处理,不必当场全部解决。
- **双指缩放行高,转为长期手势** —— 课表上双指捏合后顶栏出现 ✓ 与 ↺:✓ 保留新行高并跨启动生效,↺ 回到上次确认的尺寸。
- **设置新增实验室分组**(默认全部关闭):
  - 自适应课表高度 —— 网格填满可用空间,不再固定行高。
  - 晚间课自动隐藏 —— 晚间起始时间由你自己定。
- **桌面卡片三天窗口** —— 最小档卡片可显示滚动三天窗口(今日居首或居次),可跨上下周。
- **课程组配色回归原位** —— 组色编辑回到课程基础信息卡,开关文案改为「使用自定义颜色」;编辑页逐卡颜色行保留。
- **新学校**:西南交通大学课表、广西农业职业技术大学;另新增 WakeUp 兼容闹钟协议族八个学校族。
- **关于页 QQ 交流群入口** —— 一键复制群号并拉起 QQ。
- **学校列表顶部新增自定义 URL 导入位** —— 学校暂未收录时,直接粘贴教务系统地址尝试导入。
- **全 app 弹窗按钮重设计** —— 色块按钮边界清晰;放得下时排一排,放不下自动改全宽竖排,文字永不截断。
- **非常规时间课的占位节次** —— 窄占位行(如只溢出 5 分钟)渲染为低调灰块;点按展开到正好显示完时间文字,再点折叠。长空隙照常显示时间。

## 优化与修复

- 合肥工业大学(EAMS5)采集四段链补全。
- 返回课表不再出现周次来回跳变闪烁。
- 新建节次表取消后不再遗留空壳行。
- 草稿恢复保留完整导入配置;草稿箱入口全线接通。
- 多个伙伴协议解析修正经跨仓核验(日期偏移、rowspan、未知课程兜底)。
- 课程组色与逐卡颜色编辑互不覆盖。
- QQ 群拉起改为直开 QQ 群资料卡(此前深链可能静默失败);未安装 QQ 时自动降级。
- 编辑共用节次后,作息表预览逐门列出受影响课程的具体时间变化,不再误报"0 门课发生变化"。

## 致谢

本版本经 43 个开源高校抓取仓库交叉验证(西南交大课表 11 仓、WakeUp 闹钟协议族 32 仓),许多协议细节以其实现为参照确认——感谢他们。
