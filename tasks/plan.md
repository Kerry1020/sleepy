# v1.0.56 七项需求实现计划

基线: main @ 7ddb6254(分支另含 edbf1f69 教务修复,不冲突)· 分支 `feat/v1.0.56-seven`(worktree /private/tmp/sleepy-v1056-wt)· 主仓 ~/sleepy 有并行会话脏文件,禁止在那边动手。

## 架构决策

1. **全局唯一名是应用层约束,不动 DB schema**(period_tables/time_tables 都无 UNIQUE(name),Room 加唯一索引要迁移+老数据撞名会炸)。落点:repo 层新增 `isNameTaken(name, excludeTableId, excludePeriodTableId)` 查全库两表;所有手动命名入口(复制弹窗/作息表编辑页名称框/课表编辑页名称框/新建)保存前调它,撞名拒存+错误文案;导入自动建表仍走后缀顺延,但**预览框阶段**就把顺延后的名字算出来给用户看。
2. **第三 Tab 在 TimeSlotEditor 组件内做**,新增可选参数组(不传=两 Tab,旧行为;传=三 Tab)。四个调用点全传新参数:JwImportActivity(绑定=选一张,不选=教务解析节次)、ImportSheet 预览框(同)、EditTableScreen(绑定卡整体拆掉,选中态+未绑定项收进 Tab)、PeriodTableEditScreen(排除自己;选中=把该表节次取入当前编辑内容,非活绑)。
3. **纯作息导入走 ImportPreview 之外的独立轻路径**:SleepyNativeParser 已支持 0 C 行成功(P 块独立),parse 成功且 `courses.isEmpty() && periodTable != null` = 纯作息 → 直接弹独立的「导入作息表」确认弹窗(名称框预填+顺延后缀+预览列表),确认后 `insertPeriodTable` + 成功提示,**不建空课表**。
4. **作息表单独导出**:SleepyNativeExporter 加 `exportPeriodTableOnly`(纯 P 块 sleepy-v1 文本: magic+T空+z|chk)与 `exportPeriodTableJson`(JSON 载体: {name,nodesPerDay,time[]});PeriodTableEditScreen TopBar 加分享键 → 选择格式弹窗 → 走现有 shareText 机制。
5. **捏放手势开关只挂/不挂手势**,已存数值不动:CourseTreeView 增 `pinchEnabled: Boolean` 参数,内部 verticalResizeGesture 的 modifier 条件化;ScheduleScreen 从 AppPrefs 读新 KEY_GRID_PINCH_ZOOM(默认 false)传入;实验室分组加 SettingToggleRow。
6. **语言折叠**:GeneralSettingsScreen 语言卡改折叠卡(rememberSaveable collapsed 态,收起显示当前语言名,点开 5 项)。
7. **改名「作息表」**:17 键 ×6 locale 全改;values/values-zh-rCN/values-zh-rTW 改「作息表」系,en/ja/es 保留原译法只把「时间表」字面理顺。feature-baseline.md 同步(6 处)。StringsKeyParityTest 的 periodTableKeys 清单加新键。

## 任务序列(依赖序)

```
T1 字符串改名(纯文案,零逻辑) → T2 唯一名基建(repo查询+UI错误文案)
T3 捏放开关(AppPrefs+实验室UI+手势条件化)
T4 语言折叠(设置页)
T5 TimeSlotEditor 三Tab组件化(参数+BindTab UI)
T6 四调用点接线(EditTable拆绑定卡/双导入框/作息表编辑页)
T7 管理页(新建作息表卡+列表删键挪编辑页底)
T8 复制改名弹窗(顺延预填+确认才建+留管理页)
T9 纯作息导入路径(检测+确认弹窗+入库+预览)
T10 混合导入自动建作息表(JW路径+ImportSheet P块/wakeUpJson time→建表绑定+预览框名称顺延可见)
T11 作息表单独导出(sleepy-v1+JSON 两格式)
T12 测试全量+lint+基线文档同步
```

Checkpoint: T2 后(唯一名查询+单测绿) · T6 后(三Tab全UI接线编译过) · T12(全量1830测试+lint 新增为零)

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| EditTableScreen 拆绑定卡改坏 pendingBind 保存链 | pendingBind 状态保留原位,只挪展示进 Tab;保存逻辑零改动 |
| JwImportActivity 确认框在 AlertDialog 里塞三Tab列表挤爆 | 作息表列表 maxHeight+scroll,复用 BindOptionRow 紧凑行高 |
| 导入建表名顺延与用户编辑竞态 | 预览框显示顺延名,确认时再校验一次兜底(用户改回撞名→报错不落库) |
| 老数据已有重名(用户历史同名作息表) | 只拦新建/改名,不清洗存量;isNameTaken(exclude self) 保证编辑自己不算撞 |
| strings 改动爆 parity 测试 | 新键全部 6 locale 同步;改值不改键名,parity 只查键存在性 |

## 验证标准(全局)

- `./gradlew :app:assembleDebug` 0 error
- `./gradlew :app:testDebugUnitTest` 全绿(基线 1830,含新增)
- lint: 新增告警为 0(以 main@7ddb6254 为基线 diff)
- 模拟器: 七项逐条人工验证(截图存证,APK 指纹先行核对——并行会话共用模拟器的包身份战争教训)
