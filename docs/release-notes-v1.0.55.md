# Sleepy v1.0.55

> The app now opens in grid view with the date shown in the header, and course times read as real clock times out of the box. Every widget has been rebuilt around fixed-size windows — resizing a widget switches it to a matching layout instead of stretching or clipping it. Widgets now refresh themselves at class boundaries, and scrolling is now a per-widget choice.

## What's New

### Widgets rebuilt on fixed-size windows

- Thirteen widget sizes to place: five forms (Today, Two-Day, Week List, Week View, Week Grid) in small and regular sizes, plus three new wide layouts — Today, Two-Day and Week List now come in a short-and-wide shape that fits a single home-screen row.
- A widget now draws for its own size tier. Dragging it from a tall shape to a short one swaps in the compact layout instead of squeezing the tall one, so nothing is stretched, clipped or left half-visible.
- The Today and Two-Day widgets move their day navigation to a fixed bar at the bottom: previous day / back to today / next day, with the date and weekday in the header. A small "+N" pill sits at the bottom-left and counts the classes that didn't fit — per column on the Two-Day widget, so "today +2, tomorrow +1" reads as two separate numbers. Classes you've already attended don't count; a finished day shows "+0"; a day without classes shows no pill at all.
- Tighter spacing: at the same size, the Today widget now shows as many classes as the Two-Day widget does — two when they fit, three when there's room.
- The small Week List widget now shows the next three days in the same compact layout as the small Week View widget.

### Scrolling is now per-widget (experimental)

Scrolling is no longer one switch for all widgets. Each widget's edit page has its own "Force scroll (experimental)" toggle, off by default. Off means the widget shows what fits and reports the rest with the "+N" pill; on means that one widget scrolls its content. A scroll-enabled widget squeezed to its smallest size shows only today's classes.

### Widgets refresh at class boundaries

Each widget schedules an alarm for the next class start and end, so the in-progress highlight and the "+N" count stay accurate the moment a class changes — no waiting for a periodic refresh.

### New factory defaults

- The app opens in grid view (it used to open in week view).
- The grid header shows each day's date by default — no more hunting for where the date is.
- Course times display as clock times (08:00–09:35) by default instead of period numbers.
- These are factory defaults only: anything you've changed in settings stays exactly as you set it.

### 172 more schools for direct import

Added 172 schools surveyed from the WakeUp community's dedicated-school library, after verifying each school's portal is alive and routable. The 温州医科大学 entry now points at the school's new academic-system address.

### Change a group's shared color

Editing any course in a group (courses that share the same name and are colored together) now offers a "follow group color" row with a change-color entry. Picking a color opens the palette and a confirmation dialog (changing the group color applies to every session in the group that follows it), and sessions with their own manual color keep it. Without a custom group color the row shows "auto (golden-angle)" and a neutral swatch.

### UCAS desktop mode actually switches (issue #18)

Tapping the desktop-site toggle in the direct-import browser used to leave SEP (sep.ucas.ac.cn) on the phone layout. The page's layout is driven by its CSS breakpoint at 980px, not by the user agent, so swapping the UA string changed nothing. Desktop mode now pins the page viewport to 1024px after the page loads, which flips SEP to its desktop grid; the browser keeps the phone layout everywhere else.

### Schedule stops jumping between weeks

Switching between two timetables could leave the week view flickering back and forth across several weeks. The two-way sync between the pager and the week state fed back into itself during programmatic scrolling. Scrolling is now marked as program-driven for its duration, and the week stays where it was asked to go.

### Custom theme cards match the presets

A custom theme card used to be taller than the five preset cards, because the edit button on it carried a minimum touch-target size. The edit entry moved to the right end of the color-swatch row as a small block, and the card now has exactly the same size and spacing as the presets.

### Following-system dark mode reacts instantly

With appearance mode set to "follow system", switching the system between light and dark didn't change the app until restart, because the Compose snapshot froze at launch. The app now follows the switch immediately within the same session. This also fixes a startup crash introduced by an intermediate test build that read resources before they were attached.

## Fixes

- Widgets no longer report false conflicts: a course with its own time (e.g. a lab running 08:00–09:45) is now compared against real clock times, so it no longer collides with regular periods it doesn't actually overlap (issues #37, #32).
- The red error banner after a failed academic-system import can now be dismissed, and it clears itself as soon as the import moves to a new stage (issue #27).
- 温州医科大学 direct import now uses the school's new academic-system domain; the old address no longer resolves (issue #35).
- 华东政法大学 import fixed: the school entry pointed at a portal site that isn't the academic system; it now points at the real EAMS system.
- Honor/MagicOS (issue #31): the prev/next arrows switch days instead of opening the app; widget layouts re-render immediately when the system font size changes; a "problem loading widget" error caused by an unsupported view type in the footer is fixed.
- Fixed the week view flickering across weeks after switching timetables.
- Following-system light/dark now reacts within the session, and the startup crash from the intermediate test build is fixed.
- The UCAS desktop-mode toggle now actually switches SEP to its desktop layout (issue #18).

## Known Limitations

- The per-widget scroll toggle is experimental. With it off, classes that don't fit are counted in the "+N" pill rather than shown; enlarge the widget or enable scrolling to see them.
- The UCAS desktop layout is applied by pinning the viewport after page load; if a page re-renders late, tapping the in-app refresh button re-applies it.
- Honor/MagicOS behavior was verified via the reporter's logs and layout reasoning, not on a physical device in hand.

## Verification

- Tests: full suite 1792 tests / 0 failures / 0 errors (166 test classes), including widget geometry, fixed-window tiering, "+N" counting, per-widget scroll, date navigation, clock-time conflict clustering, i18n key-lock and school-list contracts.
- APK SHA-256:
  - arm64-v8a: 12cdc4fe342f8a56edcf1d17da6978392fc6eb422466e1321da10afcf010ba8b
  - armeabi-v7a: 4fd7e70f88ed9020a96289cf15b031ffc75e3c0828bb9885f8bf242bbf5d3880
  - x86_64: f122df67949423d964c0a7aadc431cfd2882c0c5986aa30ecbd9c1486841de4c
- Build: versionName 1.0.55 / versionCode 61

---

# Sleepy v1.0.55

> 应用现在默认以网格视图启动,表头直接显示日期,课程时间出厂即显示真实钟点。所有小组件围绕固定窗口重做:拖拽改变尺寸时切换到对应档位的布局,不再拉伸或裁切内容。小组件会在上课/下课边界自动刷新,滚动也改成了每个组件自己说了算。

## 新增功能

### 小组件按固定窗口重做

- 可添加的小组件共十三档:今日、最近两天、本周列表、周视图、网格五种形态各有小/常规两档,另新增今日、最近两天、本周列表的「宽档」——矮而宽,正好放下一排手机桌面。
- 每一档按自己的目标尺寸绘制。把组件从高的形状拖成矮的形状,会换上紧凑布局,不再把高布局压扁、裁切或显示半截。
- 今日和最近两天组件的翻日期控件移到底部固定条:上一天 / 回到今天 / 下一天,日期和星期在头部显示。左下角有一枚「+N」小胶囊,报的是没显示出来的课节数——最近两天按列各报,「今天 +2、明天 +1」是两个独立的数;已经上完的课不计入,全部上完显示「+0」,没课的那天不出现胶囊。
- 排布更紧凑:同样大小下,今日组件能和最近两天组件显示一样多的课——放得下两节就两节,放得下三节就三节。
- 本周列表小组件(小)改为与周视图(小)同构的近三天紧凑布局。

### 滚动改为每个组件独立控制(实验)

滚动不再是全局一刀切。每个组件的编辑页有自己的「强制滚动(实验)」开关,默认关。关闭时组件显示放得下的课、其余用「+N」胶囊报数;打开时仅这一个组件滚动内容。开了滚动的组件被压缩到最小档时,只显示当天的课。

### 小组件在上课边界自动刷新

每个组件会把闹钟排到下一次上课/下课的整点边界,课程进行中高亮和「+N」计数在换课瞬间就准确,不用等周期刷新。

### 出厂默认三处更新

- 应用启动默认进网格视图(以前是周视图)。
- 网格表头默认显示每天日期——不用再找日期在哪开了。
- 课程时间默认显示钟点(08:00–09:35),不再默认显示第几节。
- 以上只是出厂默认:自己在设置里改过的,永远保持你选的。

### 直连导入新增 172 所学校

依据 WakeUp 社区专属库逐校核实(门户存活且可路由),新增收录 172 所。温州医科大学条目改指向学校新的教务域名。

### 修改整组共享颜色

编辑组内任意课程(同名一起配色的一组)现在有「跟随组色」一行,带改色入口。选色先弹调色盘再弹确认对话框(组色会应用到组内所有跟随组色的节次),单独设过颜色的节次保持不动。没有自定义组色时该行显示「自动(黄金角)」和中性色块。

### UCAS 桌面模式真正切换(issue #18)

直连导入浏览器里点桌面模式按钮,SEP(sep.ucas.ac.cn)以前仍是手机布局。页面布局由 980px 的 CSS 断点驱动,与 User-Agent 无关,换 UA 字符串没有效果。现在桌面模式会在页面加载完成后把视口钳到 1024px,SEP 随之切到桌面网格;其余站点保持手机布局不变。

### 课表页不再反复跳周

两张课表间切换后,周视图可能在多个周之间来回跳变。Pager 与周次状态的双向同步在程序化滚动期间互相触发形成回路。程序化滚动全程被标记,周次停在指定位置。

### 自定义主题卡与预设卡同尺寸

自定义主题卡以前比五张预设卡高,卡上的编辑按钮带了最小点击目标尺寸。编辑入口移到色板行右端的小色块上,卡片尺寸与间隙和预设卡完全一致。

### 跟随系统深浅色即时生效

外观模式选「跟随系统」时,系统切换深浅色以前要重启 app 才生效,根子是 Compose 快照在启动时冻结。现在会话内切换立即跟随。同时修复了中间测试包在资源就绪前读取导致的启动秒崩。

## 修复

- 小组件不再误报课程冲突:带自己时间的课(如 08:00–09:45 的实验课)现在按真实钟点比对,不再和实际不重叠的常规节次撞在一起(issue #37、#32)。
- 教务导入失败后的红色报错条现在可以手动清除,且导入进入新阶段时会自动消失(issue #27)。
- 温州医科大学直连导入改用学校新的教务域名,旧地址已无法解析(issue #35)。
- 华东政法大学导入修复:原条目指向的不是教务系统门户,已改指向真正的 EAMS 教务系统。
- 荣耀/MagicOS(issue #31):翻页箭头恢复翻日期而不再打开应用;系统字号变化后组件布局立即重绘;修复页脚使用了启动器不支持的视图类型导致的「载入窗口小部件时出现问题」。
- 修复切换课表后周视图跨周反复跳变。
- 跟随系统深浅色会话内即时生效,并修复中间测试包的启动崩溃。
- UCAS 桌面模式按钮现在真正切换 SEP 桌面布局(issue #18)。

## 已知限制

- 每组件滚动开关为实验性质。关闭时放不下的课以「+N」胶囊报数而非直接显示;放大组件或打开滚动即可看到全部。
- UCAS 桌面布局靠页面加载后钳视口实现;若页面延迟重渲染,点应用内刷新按钮可重新应用。
- 荣耀/MagicOS 行为依据反馈者日志与布局推演核实,非实机复现。

## 验证

- 测试:全量套件 1792 用例 / 0 失败 / 0 错误(166 个测试类),覆盖小组件几何、固定窗口归档、「+N」计数、每组件滚动、日期导航、真实钟点冲突分簇、多语言 key 锁与学校列表契约。
- APK SHA-256:
  - arm64-v8a: 12cdc4fe342f8a56edcf1d17da6978392fc6eb422466e1321da10afcf010ba8b
  - armeabi-v7a: 4fd7e70f88ed9020a96289cf15b031ffc75e3c0828bb9885f8bf242bbf5d3880
  - x86_64: f122df67949423d964c0a7aadc431cfd2882c0c5986aa30ecbd9c1486841de4c
- 构建:versionName 1.0.55 / versionCode 61
