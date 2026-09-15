package com.lingion.sleepy.ui.screen.mine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lingion.sleepy.R
import com.lingion.sleepy.data.entity.PeriodTableEntity
import com.lingion.sleepy.data.entity.SmartPeriodConfig
import com.lingion.sleepy.ui.component.TimeSlotEditor
import com.lingion.sleepy.ui.screen.schedule.ScheduleViewModel
import com.lingion.sleepy.ui.theme.SleepyTheme
import com.lingion.sleepy.ui.theme.noRippleClickable
import com.lingion.sleepy.util.TimeTableUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 独立时间节次表编辑页(issue#40 设计 §4.2) — 复用 [TimeSlotEditor]:
 * 手动/智慧节次、保存前预览(§5.2 决策 3)、保存写 period_tables + 全部兼容列同步。
 * 编辑保存走 [ScheduleViewModel.updatePeriodTableContent](→ repo.savePeriodTable,
 * 课程行零改动); 取消预览则数据库零改动。
 *
 * @param periodTableId 目标时间表 id(管理页保证已存在; null = 无效直接返回)
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PeriodTableEditScreen(
    periodTableId: Long,
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val colors = SleepyTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val periodTables by viewModel.allPeriodTables.collectAsState()
    val scheduleState by viewModel.state.collectAsState()

    val periodTable = periodTables.find { it.id == periodTableId }
    if (periodTable == null) {
        Scaffold(containerColor = colors.background) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.edit_table_not_found), color = colors.onBackground)
            }
        }
        return
    }

    var name by remember(periodTable.id) { mutableStateOf(periodTable.name) }
    var timeSlotsExpanded by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // 预览态: 非 null = 显示确认弹窗; 确认才落库, 取消只清 state(§5.2 用户取消权)
    var pendingPreview by remember { mutableStateOf<TimeTableUtils.PeriodTablePreview?>(null) }
    var pendingSave by remember { mutableStateOf<PeriodTableEntity?>(null) }

    val slotRows = remember(periodTable.id, periodTable.updatedAt, periodTable.timeJson) {
        mutableStateListOf<TimeTableUtils.TimeSlotRow>().apply {
            addAll(TimeTableUtils.parseTimeSlotRows(periodTable.timeJson))
        }
    }
    val smartConfig = remember(periodTable.id, periodTable.smartConfigJson) {
        mutableStateOf(
            if (periodTable.smartConfigJson.isNotBlank()) {
                try {
                    Json.decodeFromString<SmartPeriodConfig>(periodTable.smartConfigJson)
                } catch (e: Exception) {
                    SmartPeriodConfig(
                        totalPeriods = slotRows.size.coerceAtLeast(1),
                        startTime = slotRows.firstOrNull()?.start?.takeIf { it.isNotBlank() } ?: "08:00"
                    )
                }
            } else {
                SmartPeriodConfig(
                    totalPeriods = slotRows.size.coerceAtLeast(1),
                    startTime = slotRows.firstOrNull()?.start?.takeIf { it.isNotBlank() } ?: "08:00"
                )
            }
        )
    }

    val fieldColors = SleepyTheme.fieldColors()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.period_tables_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // 复制时间表(§4.2): 先生成新实体再进入其编辑页, 原表与绑定关系不变
                    IconButton(onClick = {
                        scope.launch {
                            val newId = viewModel.copyPeriodTable(periodTable.id)
                            if (newId > 0) {
                                onBack()
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.period_table_copy), tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground,
                    navigationIconContentColor = colors.onBackground
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SleepyTheme.shapes.extraLarge)
                        .background(colors.surfaceContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.period_table_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SleepyTheme.fieldShape,
                        colors = fieldColors
                    )
                }
            }

            // 节次时间表(可折叠, 与 EditTableScreen 同款交互)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SleepyTheme.shapes.extraLarge)
                        .background(colors.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { timeSlotsExpanded = !timeSlotsExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.edit_table_time_slots),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.n_periods, slotRows.size) + " · " +
                                    if (timeSlotsExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.rotate(if (timeSlotsExpanded) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(
                        visible = timeSlotsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            TimeSlotEditor(
                                rows = slotRows.toList(),
                                onRowsChange = { newRows ->
                                    slotRows.clear()
                                    slotRows.addAll(newRows)
                                },
                                smartConfig = smartConfig.value,
                                onSmartConfigChange = { smartConfig.value = it }
                            )
                        }
                    }
                }
            }

            error?.let { msg ->
                item {
                    Text(text = msg, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 保存 — 先预览后确认(§5.2)
            item {
                Button(
                    onClick = {
                        val valid = slotRows.isNotEmpty() &&
                            slotRows.all { it.start.matches(Regex("\\d{2}:\\d{2}")) && it.end.matches(Regex("\\d{2}:\\d{2}")) } &&
                            slotRows.all { it.start < it.end }
                        if (!valid) {
                            error = context.getString(R.string.edit_table_validation_error)
                            return@Button
                        }
                        error = null
                        val smartConfigJson = try {
                            Json.encodeToString(smartConfig.value)
                        } catch (e: Exception) {
                            ""
                        }
                        val newTimeJson = TimeTableUtils.buildTimeJsonFromRows(slotRows.toList())
                        val updated = periodTable.copy(
                            name = name.ifBlank { periodTable.name },
                            timeJson = newTimeJson,
                            smartConfigJson = smartConfigJson,
                            nodesPerDay = slotRows.size.coerceAtLeast(1)
                        )
                        pendingSave = updated
                        // 预览: 全库范围内所有绑定本表的课程(§5.2 受影响课表列表)。
                        // 修复: state.courses 只装当前选中表的课, 直接用它统计会漏掉其他绑定表 —
                        // 从 repo 拉全库课程再按绑定表过滤
                        scope.launch {
                            val boundIds = scheduleState.tables
                                .filter { it.periodTableId == periodTable.id }
                                .map { it.id }
                                .toSet()
                            val allCourses = viewModel.getAllCourses().filter { it.tableId in boundIds }
                            val oldJson = periodTable.timeJson
                            pendingPreview = TimeTableUtils.previewPeriodTableChange(oldJson, newTimeJson, allCourses)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(SleepyTheme.Buttons.ctaHeight),
                    shape = SleepyTheme.Buttons.shape
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.edit_table_save))
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    // 保存前预览确认弹窗(§9 决策 3): 取消 = 零写库零快照
    if (pendingPreview != null && pendingSave != null) {
        val boundCount = scheduleState.tables.count { it.periodTableId == periodTable.id }
        AlertDialog(
            onDismissRequest = { pendingPreview = null; pendingSave = null },
            title = { Text(stringResource(R.string.period_table_save_preview_title), color = colors.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.period_table_preview_summary,
                            boundCount,
                            pendingPreview!!.changedCourses.size,
                            pendingPreview!!.unchangedCount
                        ),
                        color = colors.onSurfaceVariant
                    )
                    // 逐课旧时间→新时间(§5.2), 最多列 8 行防溢出
                    pendingPreview!!.changedCourses.take(8).forEach { change ->
                        Text(
                            "${change.courseName}: ${change.oldTime ?: "?"} → ${change.newTime ?: "?"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val toSave = pendingSave!!
                    pendingPreview = null
                    pendingSave = null
                    scope.launch {
                        viewModel.updatePeriodTableContent(toSave)
                        onBack()
                    }
                }) { Text(stringResource(R.string.period_table_preview_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPreview = null; pendingSave = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
