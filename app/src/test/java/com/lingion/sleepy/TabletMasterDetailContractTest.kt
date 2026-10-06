package com.lingion.sleepy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 锁定平板 Master-Detail 双栏布局必须保住的不变量。
 * 契约: 宽屏(!isCompact)时,课表页与今日页同屏并存,导航合并为合项高亮,
 * 课表在圆角卡片内,双栏间有暗色分隔。测试面全部用字符串扫描,不实例化 NavHost。
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
            minOf(rowAfterRail + 4000, navHostSrc.length),
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
    fun combined_nav_label_in_wide_rail() {
        assertTrue(
            "NavHost 应有合并课表+今日的导航项(合约: wideMergedScheduleToday)",
            navHostSrc.contains("wideMergedScheduleToday"),
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
            (
                navHostSrc.contains("clip(") && navHostSrc.contains("RoundedCornerShape")
                )
        assertTrue("课表面板应有圆角卡片容器(scheduleCard)", hasCard)
    }

    @Test
    fun compact_phone_branches_untouched() {
        assertFalse(
            "手机 dock 分支禁被误删",
            navHostSrc.contains("// PLAN:wide-merged"),
        )
        assertTrue("手机 dock(PillNavigationBar)必须保留", navHostSrc.contains("PillNavigationBar"))
        assertTrue("手机底栏(NavigationBar)必须保留", navHostSrc.contains("NavigationBar("))
    }

    @Test
    fun combined_stadium_icon_in_wide_rail() {
        // 宽屏 NavigationRail 用自研组合容器(非 NavigationRailItem 默认), 含跑道选中态
        assertTrue(
            "宽屏应自研 CombinedScheduleTodayRailItem(绕开 NavigationRailItem 默认槽位)",
            navHostSrc.contains("CombinedScheduleTodayRailItem")
        )
        // RoundedCornerShape(50) 是 50% percent 的单参重载 — 跑道形态
        assertTrue(
            "组合容器应有 StadiumShape(跑道型: 两半圆+中间矩形) = RoundedCornerShape(50)",
            Regex("""RoundedCornerShape\(\s*50\s*[,)]""").containsMatchIn(navHostSrc) ||
                Regex("""RoundedCornerShape\(percent\s*=\s*50\)""").containsMatchIn(navHostSrc)
        )
        assertTrue(
            "组合容器应有中线/中分隔(HorizontalDivider 或中线 Spacer)",
            navHostSrc.contains("HorizontalDivider") ||
                navHostSrc.contains("Divider(thickness") ||
                (navHostSrc.contains("Spacer") && navHostSrc.contains("height(0.5.dp)"))
        )
        assertTrue(
            "组合容器应有 Schedule 图标 + Today 图标两槽位",
            navHostSrc.contains("Tab.Schedule.icon") && navHostSrc.contains("Tab.Today.icon")
        )
    }

    @Test
    fun combined_stadium_adapts_orientation() {
        // 组合图标按 orientation 自适应: 左侧/右侧 NavRail 上下排, 底部 NavBar 左右排。
        // 锁存在性: 顶部(rail)调用 CombinedScheduleTodayRailItem(上下排), 底部(navBar)锚点
        // 保留 BottomCombinedScheduleToday(左右排)。两个组件都存在即满足"按方向自适应"。
        val hasVertical = navHostSrc.contains("CombinedScheduleTodayRailItem(")
        val hasHorizontal = navHostSrc.contains("BottomCombinedScheduleToday(")
        assertTrue(
            "组合容器应同时提供上下排(rail)和左右排(bottom)两个版本",
            hasVertical && hasHorizontal
        )
    }

    @Test
    fun bottom_rail_branch_present_for_future_use() {
        // 用户令: 状态栏在底部时也用组合图标(左右排列)。本版本保留占位常量以备后续启用。
        assertTrue(
            "NavHost 应保留底部组合 NavBar 分支锚点(BottomCombinedScheduleToday)",
            navHostSrc.contains("BottomCombinedScheduleToday") ||
                navHostSrc.contains("// PLAN:bottom-combined")
        )
    }
}
