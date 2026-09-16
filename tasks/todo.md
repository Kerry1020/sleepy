# v1.0.56 七项需求 todo

分支 feat/v1.0.56-seven · worktree /private/tmp/sleepy-v1056-wt · 主仓不动

## Phase 1 基建

- [ ] T1 改名「作息表」(17键×6 locale + feature-baseline.md)
  - Accept: 全库 grep 无「节次时间表/时间节次表」UI 文案;assembleDebug 绿
  - Verify: grep + assembleDebug + parity test
  - Files: 6×strings.xml, docs/sop/feature-baseline.md, StringsKeyParityTest(仅当加新键)
- [ ] T2 全局唯一名基建
  - Accept: repo.isNameTaken(两表全域查, exclude self); 单测覆盖(撞/不撞/排己)
  - Verify: 新单测红→绿
  - Files: ScheduleRepository.kt(+ScheduleViewModel 暴露), 新测试文件
- [ ] T3 捏放→实验室开关
  - Accept: KEY_GRID_PINCH_ZOOM 默认 false;关=捏不动(手势不挂),开=现行为;实验室多一行开关;存量行高不清
  - Verify: 单测 AppPrefs 默认值 + 模拟器手动验证
  - Files: AppPrefs.kt, GeneralSettingsScreen.kt, CourseTableView.kt, ScheduleScreen.kt, 6×strings
- [ ] T4 语言折叠
  - Accept: 默认收起只显当前语言;展开 5 项;选择后收起;跨页恢复
  - Verify: 模拟器
  - Files: GeneralSettingsScreen.kt

## Checkpoint A(T2-T4): 编译+全测试绿

## Phase 2 第三 Tab

- [ ] T5 TimeSlotEditor 三 Tab 组件化
  - Accept: 新可选参数(periodTables 列表/selectedId/ onSelect/排除id);不传=旧两Tab;新 Tab 列表 UI(未绑定+全部,复用 BindOptionRow 风格)
  - Verify: 编译+现有 TimeSlotEditor 相关测试零回归
  - Files: TimeSlotEditor.kt
- [ ] T6 四调用点接线
  - Accept: EditTable 拆绑定卡(未绑定/选中态原语义进 Tab);JW 确认框/导入预览框/作息表编辑页(排除自己,选中=取入内容)全有第三Tab
  - Verify: 编译+模拟器四屏逐个点
  - Files: EditTableScreen.kt, JwImportActivity.kt, ImportSheet.kt, PeriodTableEditScreen.kt, 6×strings

## Checkpoint B(T5-T6): 编译+全测试绿+模拟器四屏

## Phase 3 管理流

- [ ] T7 管理页:新建作息表卡+删除键挪编辑页
  - Accept: 卡序 导入/新建课表/新建作息表/手动/编辑/导出;列表行=编辑+复制;删除键在编辑页底部(已保存才显),拦截弹窗保留
  - Verify: 编译+模拟器
  - Files: ManagementPage.kt, PeriodTablesScreen.kt, PeriodTableEditScreen.kt, MainActivity.kt(接线), 6×strings
- [ ] T8 复制作息表弹窗
  - Accept: 复制→命名弹窗(预填顺延 2/3/4 可编辑,实时查重标错)→确认才建,留管理页;编辑页 TopBar 复制键同步改弹窗
  - Verify: 编译+单测(顺延逻辑)+模拟器
  - Files: PeriodTablesScreen.kt, PeriodTableEditScreen.kt, ScheduleViewModel/Repository, 6×strings

## Phase 4 导入导出

- [ ] T9 纯作息导入
  - Accept: sleepy-v1 P块无C行 → 独立确认弹窗(名称预填顺延+可改+查重)→确认=insertPeriodTable+提示,不建课表
  - Verify: 单测(解析0课程+periodTable非空判定)+模拟器全链
  - Files: ImportSheet.kt, 6×strings
- [ ] T10 混合导入自动建作息表
  - Accept: sleepy-v1 P+C 与 WakeUp JSON tableInfo.time → 自动建作息表(同名,撞名顺延,预览框可见后缀)+建课表+绑定;导入确认框第三Tab默认选中解析出的表
  - Verify: 单测+模拟器
  - Files: ImportSheet.kt, JwImportViewModel.kt/JwImportActivity.kt, 6×strings
- [ ] T11 作息表单独导出
  - Accept: 编辑页分享键→格式选择(sleepy-v1 文本/JSON)→shareText;导出的纯作息文本可被 T9 路径吃回(往返)
  - Verify: 单测(导出体格式+parser 吃回)+模拟器往返
  - Files: SleepyNativeExporter.kt, PeriodTableEditScreen.kt, 6×strings

## Phase 5 收口

- [ ] T12 全量验证
  - Accept: assembleDebug 0 err;testDebugUnitTest 全绿;lint 新增 0;feature-baseline.md §5.2/相关节同步七项
  - Verify: 三命令输出留证
  - Files: docs/sop/feature-baseline.md
