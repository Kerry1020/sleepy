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
}
