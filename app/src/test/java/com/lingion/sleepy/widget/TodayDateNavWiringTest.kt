package com.lingion.sleepy.widget

import com.lingion.sleepy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * 每日课程小组件日期导航 — wiring 级单测。
 *
 * v6 (2026-09-14): 删 prev/next 翻页按钮, 只留 ↻ 回到今天。用户定稿:
 * 日期左 + 居中↻按钮 (仅 !isToday 时显示)。prev/next action 常量保留 (死代码,
 * 不断 onReceive 路由兼容), 但 zones 只留 RESET。navRequestCode 保留三槽算术
 * (不删常量 = 最小化测试断链)。
 *
 * 仓库无 Robolectric, 这里覆盖:
 * 1) Action 常量字符串稳定契约 + navDelta / navRequestCode 算术;
 * 2) WidgetData.isToday 默认值 + 标题头渲染 (todayHeaderParts) 字符串分支;
 * 3) 源码级守卫: TodayWidget 管线 + 布局白名单 + emptyHeader 透传。
 */
class TodayDateNavWiringTest {

    // ---- Action 常量 + delta 映射 ----

    @Test
    fun `action constants are distinct and well-formed`() {
        val a = TodayWidgetReceiver.ACTION_PREV_DAY
        val b = TodayWidgetReceiver.ACTION_NEXT_DAY
        val c = TodayWidgetReceiver.ACTION_RESET_DAY
        assertTrue("action 须含 widget 包路径", a.startsWith("com.lingion.sleepy.widget"))
        assertTrue("action 须含 widget 包路径", b.startsWith("com.lingion.sleepy.widget"))
        assertTrue("action 须含 widget 包路径", c.startsWith("com.lingion.sleepy.widget"))
        assertFalse(a == b); assertFalse(a == c); assertFalse(b == c)
    }

    @Test
    fun `navDelta maps prev next and unknown actions`() {
        assertEquals(-1L, TodayWidgetReceiver.navDelta(TodayWidgetReceiver.ACTION_PREV_DAY))
        assertEquals(+1L, TodayWidgetReceiver.navDelta(TodayWidgetReceiver.ACTION_NEXT_DAY))
        assertNull(TodayWidgetReceiver.navDelta(TodayWidgetReceiver.ACTION_RESET_DAY))
        assertNull(TodayWidgetReceiver.navDelta(null))
        assertNull(TodayWidgetReceiver.navDelta("android.appwidget.action.APPWIDGET_UPDATE"))
    }

    // ---- R2 PendingIntent requestCode 反碰撞: (widgetId, zoneOrdinal) 全空间唯一 ----

    @Test
    fun `navRequestCode distinct for each widgetId and zone ordinal`() {
        val pairs = (1..6).flatMap { id -> (0..2).map { ord -> id to ord } }
        val codes = pairs.map { (id, ord) -> TodayWidgetReceiver.navRequestCode(id, ord) }
        assertEquals("每个 (widgetId, zoneOrdinal) 组合必须唯一", pairs.size, codes.toSet().size)
        assertFalse("navRequestCode 不能等于 widgetId 本身(会与别的 widget 撞同一个整数池)",
            TodayWidgetReceiver.navRequestCode(42, 0) == 42)
    }

    // ---- WidgetData.isToday 字段 (R6 标题渲染需) ----

    @Test
    fun `WidgetData isToday defaults to true and can be overridden`() {
        val d = WidgetData(date = LocalDate.of(2026, 9, 9), courses = emptyList(),
            timeJson = "", hasTable = false)
        assertTrue("默认 isToday=true; 已有调用方不加这个字段也能正确渲染今日态", d.isToday)
        val nav = d.copy(isToday = false)
        assertFalse(nav.isToday)
    }

    // ---- R3: todayHeaderParts 渲染字符串分支 ----

    @Test
    fun `todayHeaderParts today state shows literal title plus optional right date`() {
        val data = WidgetData(date = LocalDate.of(2026, 9, 9), courses = emptyList(),
            timeJson = "", hasTable = false, isToday = true)
        val withDate = WidgetBitmapRenderers.todayHeaderParts(
            data = data, dayName = "周三",
            showDate = true,
            resolve = { resId -> resNames.getValue(resId) }
        )
        assertEquals("today_today · 周三", withDate.title)
        assertEquals("9/9", withDate.rightText)
        assertFalse("today 右侧不是 action", withDate.rightIsAction)

        val withoutDate = withDate.let {
            WidgetBitmapRenderers.todayHeaderParts(
                data = data, dayName = "周三", showDate = false,
                resolve = { resId -> resNames.getValue(resId) }
            )
        }
        assertEquals("today_today · 周三", withoutDate.title)
        assertNull("showDate=false 时 today 右侧隐藏", withoutDate.rightText)
    }

    @Test
    fun `todayHeaderParts navigated state shows date plus dow and back-to-today affordance`() {
        val data = WidgetData(date = LocalDate.of(2026, 9, 10), courses = emptyList(),
            timeJson = "", hasTable = false, isToday = false)
        val header = WidgetBitmapRenderers.todayHeaderParts(
            data = data, dayName = "周四", showDate = true,
            resolve = { resId -> resNames.getValue(resId) }
        )
        assertEquals("9/10 · 周四", header.title)
        assertEquals("today_nav_back_to_today", header.rightText)
        assertTrue("导航态右侧必为可点击的 action 文本", header.rightIsAction)

        // showDate 不会关掉导航态右侧 (可发现性: 即便用户关了日期显示, 仍须能看到「回到今天」)
        val headerNoDate = WidgetBitmapRenderers.todayHeaderParts(
            data = data, dayName = "周四", showDate = false,
            resolve = { resId -> resNames.getValue(resId) }
        )
        assertEquals("today_nav_back_to_today", headerNoDate.rightText)
    }

    // ---- v6: 导航态不再重复显示日期 (showDate + !isToday → rightText 仍是回到今天, 非日期) ----

    @Test
    fun `todayHeaderParts navigated state never duplicates date even when showDate is true`() {
        val data = WidgetData(date = LocalDate.of(2026, 9, 10), courses = emptyList(),
            timeJson = "", hasTable = false, isToday = false)
        // showDate=true + !isToday → 旧逻辑产出 rightText="9/10" (日期重复); 新逻辑产出"回到今天"
        val header = WidgetBitmapRenderers.todayHeaderParts(
            data = data, dayName = "周四", showDate = true,
            resolve = { resId -> resNames.getValue(resId) }
        )
        assertEquals("导航态 rightText 必为回到今天, 非日期重复",
            "today_nav_back_to_today", header.rightText)
        assertTrue("导航态 rightText 必为 action", header.rightIsAction)
        assertFalse("导航态 rightText 不得是日期", header.rightText == data.dateLabel)
    }

    private val resNames = mapOf(
        R.string.today_today to "today_today",
        R.string.today_nav_back_to_today to "today_nav_back_to_today"
    )

    // ---- 源码级守卫: TodayWidget.kt 确实接上了导航管线 ----

    private fun widgetSource(name: String): File {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val f = File(dir, "app/src/main/java/com/lingion/sleepy/widget/$name")
            if (f.exists()) return f
            dir = dir.parentFile
        }
        error("$name not found")
    }

    private fun layoutFile(name: String): File {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val f = File(dir, "app/src/main/res/layout/$name")
            if (f.exists()) return f
            dir = dir.parentFile
        }
        error("$name not found")
    }

    @Test
    fun `TodayWidget source wires nav pipeline with view header`() {
        val src = widgetSource("TodayWidget.kt").readText()
        // onReceive 三 action 派发 (R2) — prev/next 常量保留但不再在 zones 注册
        assertTrue("onReceive 必须 switch 三个 nav action",
            src.contains("ACTION_PREV_DAY") && src.contains("ACTION_NEXT_DAY") &&
                src.contains("ACTION_RESET_DAY"))
        // 持久化钩子 (R4 隔离 / R3 回到今天)
        assertTrue("必须调 TodayDateNavStore.shift", src.contains("TodayDateNavStore.shift"))
        assertTrue("必须调 TodayDateNavStore.remove (回到今天)",
            src.contains("TodayDateNavStore.remove"))
        // onDeleted 必须清掉导航状态
        assertTrue("onDeleted 必须调 TodayDateNavStore.remove",
            src.contains("onDeleted") && src.contains("TodayDateNavStore.remove"))
        // pushTodayData 静态 = 真实视图顶栏容器 (bitmap 头部留白防双重标题)
        assertTrue("pushTodayData 必须用 widget_today_nav_static 静态容器",
            src.contains("widget_today_nav_static"))
        assertTrue("v4/v5/v6 均禁引用已删的 widget_scroll_today_nav (真实视图覆盖层 = ColorOS 腐坏源)",
            !src.contains("widget_scroll_today_nav"))
        assertTrue("nav 静态分支必须 emptyHeader=true (bitmap 头部留白)",
            src.contains("emptyHeader = true"))
        assertTrue("v9 overflow 必须竖排滑动 (pushScrollable + TwoDay 同构滚动层, 头部随内容滚)",
            src.contains("pushScrollable") && src.contains("widget_scroll_today"))
        assertTrue("v5 翻页机制必须已删净 (TodayPagerCore 废弃)",
            !src.contains("TodayPagerCore") && !src.contains("ACTION_PREV_PAGE"))
        assertTrue("pushTodayData 必须调 configureTodayNav",
            src.contains("configureTodayNav"))
    }

    @Test
    fun `TodayWidget source loadDataSync resolves nav target`() {
        val src = widgetSource("TodayWidget.kt").readText()
        assertTrue("loadDataSync 必须解析 nav 状态为当前 target",
            src.contains("TodayDateNavStore.target"))
        assertTrue("loadDataSync 必须把 target 当作数据日期",
            src.contains("WidgetData(date = target") || src.contains("date = target"))
    }

    @Test
    fun `nav zones in static layout use RemoteViews-whitelisted view classes`() {
        // v6: prev/next 已删, 只留 title + today(↻)
        listOf("widget_today_nav_static.xml").forEach { name ->
            val xml = layoutFile(name).readText()
            assertFalse(
                "$name 禁止裸 <View> (无 @RemoteView 注解, launcher 端 inflate 抛异常)",
                Regex("<View\\b").containsMatchIn(xml)
            )
            // v6: prev/next 已从布局删除
            assertFalse("$name 不得包含 prev 按钮 (v6 删 prev/next)",
                xml.contains("widget_today_nav_prev"))
            assertFalse("$name 不得包含 next 按钮 (v6 删 prev/next)",
                xml.contains("widget_today_nav_next"))
            // title + today(↻) 保留
            mapOf(
                "widget_today_nav_title" to "TextView",
                "widget_today_nav_today" to "ImageView",
            ).forEach { (id, expect) ->
                val idIdx = xml.indexOf("android:id=\"@+id/$id\"")
                assertTrue("$name missing nav zone $id", idIdx >= 0)
                val tagStart = xml.lastIndexOf('<', idIdx)
                val tag = Regex("[A-Za-z][A-Za-z0-9.]*")
                    .find(xml.substring(tagStart + 1))?.value
                assertEquals("$name 的 $id 必须用 $expect (RemoteViews 白名单类)",
                    expect, tag)
            }
            // ↻ 按钮大点击区 (40x28dp = 视图尺寸 = 热区)
            listOf("widget_today_nav_today").forEach { id ->
                val idIdx = xml.indexOf("android:id=\"@+id/$id\"")
                val blockEnd = xml.indexOf('>', idIdx)
                val block = xml.substring(xml.lastIndexOf('<', idIdx), blockEnd + 1)
                assertTrue("$name 的 $id 必须保留 android:clickable=\"true\"",
                    block.contains("android:clickable=\"true\""))
            }
        }
    }

    // ---- v6: configureTodayNav 不再引用 prev/next view id ----

    @Test
    fun `configureTodayNav does not reference prev or next view ids`() {
        val src = widgetSource("TodayWidget.kt").readText()
        val navBody = src.substringAfter("fun configureTodayNav(")
            .substringBefore("\n    }")
        assertFalse("configureTodayNav 不得引用 widget_today_nav_prev (v6 删 prev/next)",
            navBody.contains("widget_today_nav_prev"))
        assertFalse("configureTodayNav 不得引用 widget_today_nav_next (v6 删 prev/next)",
            navBody.contains("widget_today_nav_next"))
        assertFalse("configureTodayNav 不得渲染 renderNavTriangle (v6 删三角按钮)",
            navBody.contains("renderNavTriangle"))
    }

    // ---- v6: zones 只留 RESET (prev/next PendingIntent 不再注册) ----

    @Test
    fun `configureTodayNav zones only contain RESET action`() {
        val src = widgetSource("TodayWidget.kt").readText()
        val navBody = src.substringAfter("fun configureTodayNav(")
            .substringBefore("\n    }")
        val zonesBlock = navBody.substringAfter("val zones = listOf(")
            .substringBefore(")")
        assertFalse("zones 不得包含 ACTION_PREV_DAY (v6 删翻页)",
            zonesBlock.contains("ACTION_PREV_DAY"))
        assertFalse("zones 不得包含 ACTION_NEXT_DAY (v6 删翻页)",
            zonesBlock.contains("ACTION_NEXT_DAY"))
        assertTrue("zones 必须包含 ACTION_RESET_DAY (↻ 回到今天)",
            zonesBlock.contains("ACTION_RESET_DAY"))
    }

    // ---- v6: overflow 路径 showBackToToday=true (bitmap 显示回到今天文字, 非 today 时) ----

    @Test
    fun `overflow path shows back-to-today text in bitmap when not today`() {
        val src = widgetSource("TodayWidget.kt").readText()
        val overflowBlock = src.substringAfter("// Today 系 overflow v9")
            .substringBefore("Log.d(TAG, \"pushTodayData scroll")
        assertTrue("overflow 壳图应 showBackToToday=true (非 today 时显示回到今天文字)",
            overflowBlock.contains("showBackToToday = true"))
    }


    @Test
    fun `RemoteViewsWidgetHelper renderAndPush and pushScrollable expose configureViews hook`() {
        val src = widgetSource("RemoteViewsWidgetHelper.kt").readText()
        assertTrue("renderAndPush 必须有 configureViews 钩子接 Today 导航 zone",
            src.contains("configureViews"))
        // 两个推送路径 (static + scrollable) 都必须支持
        assertTrue("pushScrollable 也必须有 configureViews 钩子 (scrollable 今日态也需导航)",
            src.substringAfter("fun pushScrollable").contains("configureViews"))
    }

    @Test
    fun `WidgetBindingStore-onDeleted cleanup has matching nav store cleanup`() {
        // 镜像 WidgetBindingStore 的清理纪律: onDeleted 收尾必须包括今日导航 store.remove
        val src = widgetSource("TodayWidget.kt").readText()
        val onDeletedBlock = src.substringAfter("override fun onDeleted")
        assertTrue("onDeleted 必须调 WidgetBindingStore.remove (兼容旧契约)",
            onDeletedBlock.contains("WidgetBindingStore.remove"))
        assertTrue("onDeleted 必须同时清今日导航状态",
            onDeletedBlock.contains("TodayDateNavStore.remove"))
    }

    @Test
    fun `push commit points guard against stale generations`() {
        // resize 稳定性: 渲染在后台协程, resize 拖拽期间系统连发 OPTIONS_CHANGED。
        // 旧尺寸任务若后完成会覆盖新内容且无人纠正 → commit 前必须校验世代号。
        val helper = widgetSource("RemoteViewsWidgetHelper.kt").readText()
        assertTrue("pushScrollable commit 前须校验世代号 (WidgetResizeCore.isStale)",
            helper.substringAfter("fun pushScrollable").contains("WidgetResizeCore.isStale"))
        assertTrue("renderAndPush commit 前也须校验 (WeekGrid 最小档 overflow 同样受益)",
            helper.substringAfter("fun renderAndPush").contains("WidgetResizeCore.isStale"))
        val today = widgetSource("TodayWidget.kt").readText()
        assertTrue("Today receiver 三个触发点 (onUpdate/optionsChanged/nav) 须先 bump 世代",
            today.contains("WidgetResizeCore.bump"))
        val svc = widgetSource("ScrollStripService.kt").readText()
        assertTrue("条带工厂 onDataSetChanged 也须按世代丢弃过期重算",
            svc.contains("WidgetResizeCore"))
    }

    @Test
    fun `onDeleted clears resize generation for widget id reuse`() {
        val today = widgetSource("TodayWidget.kt").readText()
        assertTrue("onDeleted 必须清世代号 (widget id 被系统复用后旧世代不得干扰新实例)",
            today.contains("WidgetResizeCore.remove"))
    }

    @Test
    fun `WeekGrid minimum variant reuses pushTodayData without nav zones (scope guard)`() {        // issue #24 范围铁律: 日期导航只在每日小组件。WeekGrid 最小档复用 pushTodayData
        // 管线, 但不得获得导航布局/点击区 — 守卫 WeekGrid 侧零沾染。
        val grid = widgetSource("WeekGridWidgetProvider.kt").readText()
        assertFalse("WeekGrid 不得引用 widget_today_nav_static",
            grid.contains("widget_today_nav_static"))
        assertFalse("WeekGrid 不得引用 widget_scroll_today_nav",
            grid.contains("widget_scroll_today_nav"))
        assertFalse("WeekGrid 不得引用 configureTodayNav",
            grid.contains("configureTodayNav"))
        // 它的 5 参调用 (receiverClass 缺省 null → 导航关) 保持原样
        assertTrue("WeekGrid 调用点保持 5 参缺省形态",
            grid.contains("pushTodayData(context, awm, widgetId, WidgetVariant.SMALL, todayData)"))
    }

    private fun findUpward(rel: String): File {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val f = File(dir, rel)
            if (f.exists()) return f
            dir = dir.parentFile
        }
        error("\$rel not found")
    }

}
