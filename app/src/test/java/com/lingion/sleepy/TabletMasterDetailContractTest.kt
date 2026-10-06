package com.lingion.sleepy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 锁定平板 Master-Detail 双栏布局必须保住的不变量。
 * 契约: 宽屏(!isCompact)时,左 50% 永远 ScheduleScreen;
 * 右 50% 跟用户选中的 rail 项切换 Today/Manage/Mine;
 * NavigationRail 严格 3 项,每项都是 Schedule+X 跑道胶囊 (无 label)。
 * 测试面全部用字符串扫描,不实例化 NavHost。
 */
class TabletMasterDetailContractTest {

    private val navHostSrc by lazy {
        File("src/main/java/com/lingion/sleepy/ui/nav/SleepyNavHost.kt").readText()
    }
    private val todayScreenSrc by lazy {
        File("src/main/java/com/lingion/sleepy/ui/screen/today/TodayScreen.kt").readText()
    }

    @Test
    fun wide_screen_has_two_pane_layout() {
        assertFalse("PLAN 标记必须清除(实现已落地)", navHostSrc.contains("// PLAN:wide-merged"))
        assertTrue("宽屏分支应有 Row 双栏容器", navHostSrc.contains("if (!isCompact)"))
        val railSection = navHostSrc.indexOf("NavigationRail")
        assertTrue("宽屏导航锚点 NavigationRail 必须存在", railSection >= 0)
        val rowAfterRail = navHostSrc.indexOf("Row(", railSection)
        assertTrue("NavigationRail 之后应有 Row 双栏布局", rowAfterRail > railSection)
        val rowBody = navHostSrc.substring(
            rowAfterRail,
            minOf(rowAfterRail + 8000, navHostSrc.length),
        )
        assertTrue("宽屏 Row 内必须有 ScheduleScreen 调用", rowBody.contains("ScheduleScreen("))
    }

    @Test
    fun today_pane_exported_for_tablet_use() {
        assertTrue(
            "TodayScreen 应导出平板用紧凑面板(CompactTodayPane)",
            todayScreenSrc.contains("fun CompactTodayPane"),
        )
        assertTrue(
            "NavHost 宽屏分支应调用 CompactTodayPane",
            navHostSrc.contains("CompactTodayPane("),
        )
    }

    @Test
    fun panes_have_dark_gap() {
        val hasGap = navHostSrc.contains("surfaceContainerLow") ||
            navHostSrc.contains("HorizontalDivider(") ||
            navHostSrc.contains("Divider(")
        assertTrue("双栏应有视觉分隔(dark gap)", hasGap)
    }

    @Test
    fun schedule_pane_in_rounded_card() {
        val hasCard = navHostSrc.contains("scheduleCard") ||
            (navHostSrc.contains("clip(") && navHostSrc.contains("RoundedCornerShape"))
        assertTrue("课表面板应有圆角卡片容器(scheduleCard)", hasCard)
    }

    @Test
    fun compact_phone_branches_untouched() {
        assertFalse("手机 dock 分支禁被误删", navHostSrc.contains("// PLAN:wide-merged"))
        assertTrue("手机 dock(PillNavigationBar)必须保留", navHostSrc.contains("PillNavigationBar"))
        assertTrue("手机底栏(NavigationBar)必须保留", navHostSrc.contains("NavigationBar("))
    }

    @Test
    fun rail_has_exactly_three_combined_capsules() {
        // 匹配 "CombinedRailItem(" 调用形式 (排除函数定义 "private fun CombinedRailItem(")
        val count = Regex("""\bCombinedRailItem\(\s*\w+\s*=""").findAll(navHostSrc).count()
        assertEquals("Rail 严格 3 个胶囊", 3, count)
    }

    @Test
    fun left_half_is_permanent_schedule() {
        // 截 NavigationRail 关闭 } 后 4000 字符,该区段必须包含 ScheduleScreen 与 viewModel = mainVm
        val railIdx = navHostSrc.indexOf("NavigationRail {")
        assertTrue("必须存在 NavigationRail { ... }", railIdx >= 0)
        // 找第一对大括号闭合 (lambda body 用大括号, 第二对匹配 NavigationRail 自身)
        val railBodyStart = navHostSrc.indexOf("{", railIdx)
        var depth = 0
        var bodyEnd = -1
        var i = railBodyStart
        while (i < navHostSrc.length && i < railIdx + 4000) {
            when (navHostSrc[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) { bodyEnd = i; break } }
            }
            i++
        }
        assertTrue("必须闭合 NavigationRail", bodyEnd >= 0)
        val afterRail = navHostSrc.substring(bodyEnd, minOf(bodyEnd + 4000, navHostSrc.length))
        assertTrue("左半 ScheduleScreen 永久固定", afterRail.contains("ScheduleScreen("))
        assertTrue("左半 viewModel = mainVm 共享", afterRail.contains("viewModel = mainVm"))
    }

    @Test
    fun right_half_animated_content() {
        assertTrue("右半边用 AnimatedContent", navHostSrc.contains("AnimatedContent("))
        assertTrue(
            "右半边 fadeIn+togetherWith+fadeOut 200ms",
            Regex("""fadeIn\(.*tween\(200\).*togetherWith.*fadeOut\(.*tween\(200""")
                .containsMatchIn(navHostSrc),
        )
    }

    @Test
    fun no_label_on_combined_capsule() {
        val start = navHostSrc.indexOf("private fun CombinedRailItem(")
        assertTrue("CombinedRailItem 必须存在", start >= 0)
        val body = navHostSrc.substring(start, minOf(start + 4000, navHostSrc.length))
        assertFalse(
            "胶囊内部不应有 Text(text = label) 调用",
            Regex("""Text\(\s*text\s*=\s*label""").containsMatchIn(body),
        )
    }

    @Test
    fun deleted_legacy_anchors() {
        assertFalse("tab_schedule_today 字符串应已删除", navHostSrc.contains("tab_schedule_today"))
        assertFalse("BottomCombinedScheduleToday 应已删除", navHostSrc.contains("BottomCombinedScheduleToday"))
        assertFalse("PLAN:bottom-combined 注释应已删除", navHostSrc.contains("// PLAN:bottom-combined"))
        assertFalse("CombinedScheduleTodayRailItem 应已删除", navHostSrc.contains("CombinedScheduleTodayRailItem"))
    }
}
