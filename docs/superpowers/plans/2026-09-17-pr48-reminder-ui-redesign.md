# PR48 每日提醒区重设计 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 PR48 合并进 `integrate/pr48-tomorrow-reminder` 的每日提醒设置区改成单卡母子形态(卡头总开关沿用旧键),子项采用「今日摘要 / 明日预告」预告版文案,前一晚无课推送使用明日专用文案。

**Architecture:** 纯 UI 层重排 + 文案键替换 + 新增一个通知文案键;调度语义(AppPrefs 键、Receiver、Scheduler)零改动。两卡(时间卡+开关卡)合一卡,卡头开关 = `daily_reminder` 旧键,打开后展开两个子项块,每块 = 子开关 + 时间行 + 示例。

**Tech Stack:** Jetpack Compose (Material3), Android string resources ×6 locale, JUnit JVM 契约测试。

## Global Constraints

- 分支: `integrate/pr48-tomorrow-reminder` (本地,未 push)
- 老用户行为不变: `daily_reminder` 默认 true + `today_reminder` 默认 true → 升级后当天提醒照常发
- 六语言(values / values-en / values-es / values-ja / values-zh-rCN / values-zh-rTW)必须同步改,`StringsKeyParityTest` 的 tomorrowReminderKeys 列表同步更新
- 禁 BorderStroke/OutlinedButton;开关 Switch 用主题色(既有代码模式)
- commit 作者 `lingion <lingion@hrbeu.edu.cn>`,无 Co-Authored-By
- UI 纯色块规则: 本任务不新增按钮,只用既有 ReminderCard/SubDivider/ReminderTimeRow/ReminderToggleRow 组件

## 文案键变更总表(六语言同步)

| 动作 | 键 | 新文案 (zh) | 新文案 (en) |
|---|---|---|---|
| 改 | reminder_daily_sub | 每天定时推送今日摘要与明日预告 | Set daily course summaries: today's and tomorrow-evening preview |
| 改 | reminder_daily_time_label | 今日摘要时间 | Today summary time |
| 改 | reminder_daily_preview | 示例: 今日 15 号 您有 3 节课 第一节课 高等数学 于 08:00 在 教学楼A101 上课 | Example: Today the 15th, 3 classes. First class 08:00 Advanced Math @ Bldg A101 |
| 改 | reminder_tomorrow_time_label | 明日预告时间 | Tomorrow preview time |
| 改 | reminder_tomorrow_preview | 示例: 明日 16 号 您有 3 节课 第一节课 高等数学 于 08:00 在 教学楼A101 上课 | Example: Tomorrow the 16th, 3 classes. First class 08:00 Advanced Math @ Bldg A101 |
| 改 | reminder_daily_today_toggle_title | 今日摘要 | Today summary |
| 改 | reminder_daily_today_toggle_sub | 在设定时间推送今日课程摘要 | Send today's course summary at the set time |
| 改 | reminder_tomorrow_toggle_title | 明日预告 | Tomorrow preview |
| 改 | reminder_tomorrow_toggle_sub | 前一天晚上推送明日课程摘要 | Send tomorrow's summary the previous evening |
| 增 | notif_tomorrow_text_no_course | 明日无课,好好休息! | No classes tomorrow. Take a break! |
| 删 | reminder_daily_switches_title | (整卡删除) | |
| 删 | reminder_daily_master_toggle_title | (行移入卡头) | |
| 删 | reminder_daily_master_toggle_sub | (行移入卡头) | |

es/ja/zh-rTW 按 en/zh 语义对译(参照各语言现有 reminder_* 文案风格)。

## UI 目标形态(ASCII)

```
┌ 每日提醒 (icon AccessTime) ────────⬤┐   ← 卡头: 标题+副题+Switch(旧键 dailyEnabled)
│ 每天定时推送今日摘要与明日预告        │
├──────────────────────────────────────┤   ← dailyEnabled=false 时,以下整块隐藏
│ 今日摘要                         ⬤   │   ← todayEnabled 开关
│   今日摘要时间              07:00    │   ← 点开 TimePicker(target=Today)
│   示例: 今日15号 您有3节课...        │
├──────────────────────────────────────┤
│ 明日预告                         ⬪   │   ← tomorrowEnabled 开关(默认关)
│   明日预告时间              22:00    │   ← 点开 TimePicker(target=Tomorrow)
│   示例: 明日16号 您有3节课...        │
└──────────────────────────────────────┘
```

子项内部开关与时间行常显(不随子开关隐藏)——用户关着也能看到示例和时间,知道开了会得到什么。

---

### Task 1: 明日无课专用文案(6 语言 + 通知代码)

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (+1 行)
- Modify: `app/src/main/res/values-en/strings.xml` (+1)
- Modify: `app/src/main/res/values-es/strings.xml` (+1)
- Modify: `app/src/main/res/values-ja/strings.xml` (+1)
- Modify: `app/src/main/res/values-zh-rCN/strings.xml` (+1)
- Modify: `app/src/main/res/values-zh-rTW/strings.xml` (+1)
- Modify: `app/src/main/java/com/lingion/sleepy/widget/notification/CourseNotificationScheduler.kt:~540` (sendScheduleSummary 无课分支)
- Test: `app/src/test/java/com/lingion/sleepy/TomorrowReminderWiringContractTest.kt`

**Interfaces:**
- Produces: string key `notif_tomorrow_text_no_course` (6 locales)

- [ ] **Step 1: 写失败测试** — 在 `TomorrowReminderWiringContractTest` 的 `both reminder types publish a no course summary` 测试中追加断言:

```kotlin
assertTrue(
    "前一晚无课摘要必须用明日专用正文,不能复用「享受一天」",
    source.contains("R.string.notif_tomorrow_text_no_course")
)
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lingion.sleepy.TomorrowReminderWiringContractTest"`
Expected: FAIL (键不存在/代码未引用)

- [ ] **Step 3: 六语言加键** — 各 locale 在 `notif_tomorrow_title_no_course` 后加:

| locale | value |
|---|---|
| values | 明日无课,好好休息! |
| values-en | No classes tomorrow. Take a break! |
| values-es | Mañana no hay clases. ¡Descansa! |
| values-ja | 明日は授業がありません。ゆっくり休んでください! |
| values-zh-rCN | 明日无课,好好休息! |
| values-zh-rTW | 明日無課,好好休息! |

- [ ] **Step 4: 通知代码切换** — `sendScheduleSummary` 无课分支 text 改为:

```kotlin
text = context.getString(
    if (isTomorrowPreview) R.string.notif_tomorrow_text_no_course
    else R.string.notif_daily_text_no_course
)
```

- [ ] **Step 5: 跑测试确认通过** (同 Step 2 命令,Expected: PASS)

- [ ] **Step 6: Commit** `feat(reminder): 前一晚无课摘要改用明日专用正文 (PR48 落地调整)`

---

### Task 2: 预告版文案重命名(6 语言改 9 键 + parity 测试列表同步)

**Files:**
- Modify: 6 个 strings.xml(按上面总表「改」的 9 个键)
- Test: `app/src/test/java/com/lingion/sleepy/StringsKeyParityTest.kt` (tomorrowReminderKeys 列表不变——键名没变,只有 value 变;但需人工确认 9 键 value 全改)

**Interfaces:**
- Produces: 9 个既有键的新 value(键名不变,代码零改动)

- [ ] **Step 1: 逐语言改 value**(zh 值见总表;en 值见总表;es/ja/zh-rTW 对译,风格对齐各语言现行 reminder_* 文案)
- [ ] **Step 2: 跑 parity 测试** — `./gradlew :app:testDebugUnitTest --tests "com.lingion.sleepy.StringsKeyParityTest"` Expected: PASS(键集合未变)
- [ ] **Step 3: Commit** `feat(reminder): 每日提醒文案改预告版 — 今日摘要/明日预告 (PR48 落地调整)`

---

### Task 3: 单卡母子布局(卡头开关 + 展开子项)

**Files:**
- Modify: `app/src/main/java/com/lingion/sleepy/ui/screen/mine/ReminderScreen.kt:~223-313`(每日提醒两个 item 合一)
- Modify: 6 个 strings.xml(删 reminder_daily_switches_title / reminder_daily_master_toggle_title / reminder_daily_master_toggle_sub 三键)
- Test: `app/src/test/java/com/lingion/sleepy/StringsKeyParityTest.kt`(tomorrowReminderKeys 列表删去三键)

**Interfaces:**
- Consumes: Task 2 的 9 个新 value;既有组件 ReminderCard / SubDivider / ReminderTimeRow / ReminderToggleRow
- Produces: UI 形态 = 卡头 Switch(dailyEnabled) + `if (dailyEnabled)` 内两个子块;AppPrefs 键与调度调用完全不变

- [ ] **Step 1: 改 parity 测试列表** — tomorrowReminderKeys 删 3 键,跑 `StringsKeyParityTest` 确认仍 PASS(3 键已从 strings 消失,列表不再要求)

- [ ] **Step 2: 重排 ReminderScreen** — 「每日提醒」item 改为:

```kotlin
item {
    ReminderCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(icon = Icons.Outlined.AccessTime, color = colors.primary)
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reminder_daily_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(R.string.reminder_daily_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Switch(
                checked = dailyEnabled,
                onCheckedChange = { enabled ->
                    dailyEnabled = enabled
                    AppPrefs.setDailyReminderEnabled(context, enabled)
                    SleepyApp.get().notificationScheduler.scheduleAll()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary
                )
            )
        }

        if (dailyEnabled) {
            SubDivider()
            ReminderToggleRow(
                title = stringResource(R.string.reminder_daily_today_toggle_title),
                subtitle = stringResource(R.string.reminder_daily_today_toggle_sub),
                checked = todayEnabled,
                onCheckedChange = { enabled ->
                    todayEnabled = enabled
                    AppPrefs.setTodayReminderEnabled(context, enabled)
                    SleepyApp.get().notificationScheduler.scheduleAll()
                }
            )
            ReminderTimeRow(
                label = stringResource(R.string.reminder_daily_time_label),
                time = dailyTime,
                onClick = { timePickerTarget = DailyReminderTimeTarget.Today }
            )
            Text(
                text = stringResource(R.string.reminder_daily_preview),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp, end = 4.dp)
            )
            SubDivider()
            ReminderToggleRow(
                title = stringResource(R.string.reminder_tomorrow_toggle_title),
                subtitle = stringResource(R.string.reminder_tomorrow_toggle_sub),
                checked = tomorrowEnabled,
                onCheckedChange = { enabled ->
                    tomorrowEnabled = enabled
                    AppPrefs.setTomorrowReminderEnabled(context, enabled)
                    SleepyApp.get().notificationScheduler.scheduleAll()
                }
            )
            ReminderTimeRow(
                label = stringResource(R.string.reminder_tomorrow_time_label),
                time = tomorrowTime,
                onClick = { timePickerTarget = DailyReminderTimeTarget.Tomorrow }
            )
            Text(
                text = stringResource(R.string.reminder_tomorrow_preview),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp, end = 4.dp)
            )
        }
    }
}
```

删除原「Daily reminder switches deliberately live below time settings.」整个 item。卡头形态对齐同屏「每节课前提醒」卡(Row + weight(1f) + Switch)。

- [ ] **Step 3: 删 strings 三键**(6 语言同步删 reminder_daily_switches_title / reminder_daily_master_toggle_title / reminder_daily_master_toggle_sub)

- [ ] **Step 4: 编译 + 全量测试**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: 全绿,总数不变(1971)

- [ ] **Step 5: Commit** `refactor(reminder): 每日提醒区单卡母子 — 卡头总开关+子项展开, 删独立开关卡 (PR48 落地调整)`

---

### Task 4: 终验 + lint 基线复核

- [ ] **Step 1: 全量测试** — `./gradlew :app:testDebugUnitTest` Expected: 1971+7=0 失败(1971 不含 Task1 新增断言数,以实际为准,0 失败为闸)
- [ ] **Step 2: lint** — `./gradlew :app:lintDebug || true` 解析 lint-results-debug.xml:Error=38(main 基线),PR48 新键(reminder_tomorrow*/notif_tomorrow*)在 warning 里命中=0
- [ ] **Step 3: 静态自查 ASCII 形态** — 对照设计稿确认: 卡头关→子项隐藏;两个 TimePicker target 分支未动;`showTimePicker` 无残留引用
- [ ] **Step 4: 汇报** — 分支停在本地,push/合并 main 等用户批准

## 验证清单(汇报时逐条给证据)

1. 六语言 parity: StringsKeyParityTest PASS
2. 契约: TomorrowReminderWiringContractTest PASS(含新增明日文案断言)
3. 全量: 0 failures
4. lint: Error 38 = 基线,新键 0 命中
5. 老用户: daily_reminder/today_reminder 默认值未动(代码 diff 中无 AppPrefs 改动)
