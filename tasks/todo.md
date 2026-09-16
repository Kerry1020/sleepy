# Todo: 独立时间节次表（Issue #40）

## Task 1: Period-table entity + DAO + Room migration + undo snapshot

**Description:** 新增 `PeriodTableEntity`（period_tables 表）、`PeriodTableDao`，并把 AppDatabase 升版本挂 MIGRATION_6_7；迁移时为每张旧课表生成独立时间节次表并把 `TimeTableEntity.periodTableId` 指过去；扩展 UndoManager 快照覆盖 period_tables。

**Acceptance criteria:**
- [ ] `PeriodTableEntity` 含 id/name/nodesPerDay/timeJson/smartConfigJson/createdAt/updatedAt。
- [ ] `TimeTableEntity` 新增 `periodTableId: Long?`（可空，默认 null）。
- [ ] MIGRATION_6_7 为每张旧课表插入一张独立 `period_tables` 行（继承 timeJson/smartConfigJson/nodesPerDay），并回填 periodTableId。
- [ ] 撤回快照含 period_tables；恢复顺序 period_tables → time_tables → courses（外键顺序）。
- [ ] 旧数据升级后每张课表的节次、课程、绑定时间表都不变。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest --tests "*Migration*"` 0 failures
- [ ] `./gradlew :app:compileDebugKotlin` exit 0
- [ ] 手动核对迁移测试输出

**Dependencies:** None
**Files likely touched:** `data/entity/PeriodTableEntity.kt`（新）、`data/dao/PeriodTableDao.kt`（新）、`data/entity/TimeTableEntity.kt`、`data/AppDatabase.kt`、`data/Migrations.kt`、`data/repository/UndoManager.kt`、`data/repository/ScheduleRepository.kt`
**Estimated scope:** Medium (5-7 files)

## Task 2: Repository reads + one-way binding

**Description:** 仓库层新增“有效时间节次表”读取（优先 periodTableId → 独立表，异常回退旧 timeJson）；新增 bind/unbind、复制时间节次表、删除被引用时间表的守卫逻辑。

**Acceptance criteria:**
- [ ] `effectivePeriodTable(tableId)` 返回绑定时间表或 null（回退旧列）。
- [ ] `bindPeriodTable(timeTableId, periodTableId)` 仅改 periodTableId，课程行零改动。
- [ ] `copyPeriodTable(sourceId)` 新建副本，不动原绑定。
- [ ] 删除被引用时间表被拒绝；未引用可删。
- [ ] 一张时间表可绑定 2+ 张课表（测试锁）。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest --tests "*PeriodTable*"` 0 failures
- [ ] `./gradlew :app:testDebugUnitTest --tests "*ScheduleRepository*"` 0 failures

**Dependencies:** Task 1
**Files likely touched:** `data/repository/ScheduleRepository.kt`、`data/dao/PeriodTableDao.kt`、测试
**Estimated scope:** Medium (3-5 files)

## Task 3: Route all consumers through effective period table

**Description:** 逐文件盘查 timeJson 消费方（ScheduleViewModel/CourseDetailSheet/ConflictLayoutEngine/CourseNotificationScheduler/widget 系/JwImportViewModel/ScheduleExporter/parser 系），统一改走“有效时间节次表”，异常回退旧列。改前 grep 全部消费方。

**Acceptance criteria:**
- [ ] grep 显示核心渲染路径不再直接读 `TimeTableEntity.timeJson` 作为最终来源。
- [ ] 绑定切换后课表显示与时间域按新表解释。
- [ ] 自定义时间课程不受影响。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest` 全绿
- [ ] `./gradlew :app:compileDebugKotlin` exit 0

**Dependencies:** Task 2
**Files likely touched:** ScheduleViewModel.kt、CourseTableView.kt、CourseDetailSheet.kt、ConflictDetailReporter.kt、ConflictLayoutEngine.kt、CourseNotificationScheduler.kt、widget/WidgetContent.kt、widget 系其余文件、JwImportViewModel.kt、parser 系
**Estimated scope:** Large (5-8+ files，纯读取改道，无逻辑重写)

## Task 4: Preview + transactional save with cancel

**Description:** 实现时间节次表保存前的纯函数预览（新旧两份 timeJson 对同一批节次编号解析出 旧时间→新时间），确认后事务内写入并刷新全部绑定课表；取消则不写库。

**Acceptance criteria:**
- [ ] 纯函数 preview 计算：受影响课表列表 + 每课程旧时间→新时间。
- [ ] 取消不写库、不进撤回快照。
- [ ] 确认后单事务保存 period_tables + 兼容列，立即对全部绑定课表生效。
- [ ] 课程行 startNode/step 零改动（逐行 diff = 0，测试锁）。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest --tests "*Preview*"` 0 failures
- [ ] `./gradlew :app:testDebugUnitTest --tests "*PeriodTable*"` 0 failures

**Dependencies:** Task 2
**Files likely touched:** `util/TimeTableUtils.kt`（纯函数）、ScheduleRepository.kt、ScheduleViewModel.kt、测试
**Estimated scope:** Medium (3-5 files)

## Task 5: Independent period-table list + editor UI

**Description:** 我的页新增“时间节次表”入口；列表显示各时间表与“已绑定 N 张课表”；编辑页复用既有节次编辑控件（TimeSlotEditor），支持手动/智慧节次、复制时间表、保存前预览。

**Acceptance criteria:**
- [ ] 我的页出现入口，进列表后可新建/编辑/复制/删除（删除守卫同 Task 2）。
- [ ] 列表显示每张时间表已绑定课表数。
- [ ] 编辑保存走 Task 4 的预览+事务。
- [ ] strings 新 key 全 6 locale 同步（StringsKeyParityTest 锁）。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest --tests "*StringsKeyParity*"` 0 failures
- [ ] `./gradlew :app:compileDebugKotlin` exit 0

**Dependencies:** Task 4
**Files likely touched:** MineScreen.kt、MainActivity.kt（新 OverlayScreen 枚举+栈接线）、新 PeriodTablesScreen.kt、PeriodTableEditScreen.kt（复用 TimeSlotEditor）、strings ×6
**Estimated scope:** Medium (5-7 files)

## Task 6: Course-table binding selector + copy + safe delete/rebind

**Description:** EditTableScreen 加“时间节次表”选择项（下拉选表 + 复制时间节次表按钮），保存走预览确认；AllTables/删除动线补改绑守卫。

**Acceptance criteria:**
- [ ] 课程表编辑页可换绑/复制时间节次表。
- [ ] 换绑保存前预览该课表各课程时间变化，可取消。
- [ ] 删除被引用时间表先显示绑定课表并提供改绑入口。
- [ ] 复制课程表默认复用原绑定时间表。

**Verification:**
- [ ] `./gradlew :app:compileDebugKotlin` exit 0
- [ ] 相关 UI/仓库测试全绿

**Dependencies:** Tasks 2, 4
**Files likely touched:** EditTableScreen.kt、AllTablesScreen.kt、ScheduleViewModel.kt、strings ×6
**Estimated scope:** Medium (3-5 files)

## Task 7: Import/export formats + round-trip

**Description:** sleepy-v1/原生导出加可选 periodTable(s) 区块；旧格式保持每张课表各自 timeJson；导入时旧格式每张课表各建一张时间节次表，新格式恢复共享关系；往返测试锁无损。

**Acceptance criteria:**
- [ ] 新格式含 periodTable(s) 且旧版本可读（兼容 timeJson 保留）。
- [ ] 旧格式导入：每张课表独立时间表，不误共享。
- [ ] 导出导入往返：绑定关系、节次、课程无损（Extend RoundTrip 测试风格）。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest --tests "*RoundTrip*"` 0 failures
- [ ] `./gradlew :app:testDebugUnitTest --tests "*Import*"` 0 failures

**Dependencies:** Tasks 1-2
**Files likely touched:** SleepyNativeFormat.kt、SleepyNativeExporter.kt、SleepyNativeParser.kt、ScheduleExporter.kt、parser 系、测试
**Estimated scope:** Medium (4-6 files)

## Task 8: Contract tests + baseline + full verification

**Description:** 设计文档 §10 验收契约逐条落测试；feature-baseline.md 同步新实体/入口/行为；全量验证。

**Acceptance criteria:**
- [ ] §10 每条契约有对应测试或既有测试引用。
- [ ] feature-baseline.md §6/相关章新增 PeriodTable 记录。
- [ ] 全量单测+编译绿。

**Verification:**
- [ ] `./gradlew :app:testDebugUnitTest` 全绿
- [ ] `./gradlew :app:compileDebugKotlin` exit 0
- [ ] 逐条核对 §10

**Dependencies:** Tasks 1-7
**Files likely touched:** 测试、docs/sop/feature-baseline.md
**Estimated scope:** Medium
