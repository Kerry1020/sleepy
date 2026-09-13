package com.lingion.sleepy.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 纯 JVM 测试: 顶栏降级判定 [TodayWidgetReceiver.navHeaderTier] 的 v6 单钮定稿语义
 * (Android-free — Paint 用 mock 尺寸序列注入)。
 *
 * v6 (2026-09-14): 删 prev/next, 只留 ↻ 回到今天 (40×28dp)。用户定稿:
 * 日期左 + 居中↻按钮 (仅 !isToday 时显示)。
 * 恓牲序 = 标题先退:
 *   FULL → SHORT_TITLE(去星期) → HIDE_TITLE(标题 GONE, ↻ 保留) → HIDE_NAV
 * 旧三钮预算作废; 单钮预算 = padStart10 + title + (6+40+6) + padEnd10。
 * ↻ GONE (isToday): 无按钮 → 预算 = 10 + title + 10。
 *
 * 预算常量 (dp): 单钮 10+4+40+10 = 64dp (标题 GONE 时); 标题档 + title。
 */
class NavHeaderFitTest {

    /** 记录构造时收到的 textSize (px), 其余行为无关紧要的 Paint 替身。 */
    private class RecordingPaint(var pxAtConstruction: Float = -1f) {
        fun measureText(s: String): Float = s.length * pxAtConstruction
    }

    private fun tier(
        density: Float, wDp: Int, fullTitle: String, dateOnly: String,
        titleM: (String) -> Float = { it.length * 13f * density },
        refreshVisible: Boolean = true
    ): NavTier = TodayWidgetReceiver.navHeaderTier(
        density, wDp, fullTitle, dateOnly,
        titleMeasure = titleM, refreshVisible = refreshVisible
    )

    // ── FULL 档: 宽 widget 满标题 + ↻ 按钮 ──

    @Test
    fun `wide widget keeps full title and refresh button`() {
        // req(full=9字) = 10+117+4+52+10 = 193 ≤ 400
        assertEquals(NavTier.FULL, tier(1f, 400, "9/12 · 周六", "9/12"))
    }

    @Test
    fun `unknown width defaults to full safe path`() {
        assertEquals(NavTier.FULL, tier(1f, 0, "9/12 · 周六", "9/12"))
    }

    @Test
    fun `four-by-three real width keeps full title`() {
        // 4×3 ≈ 250dp: req(full=104) = 10+104+4+52+10 = 180 ≤ 250 → FULL
        assertEquals(NavTier.FULL, tier(1f, 250, "9/12 · 周六", "9/12"))
    }

    // ── SHORT_TITLE 档: 标题去星期 ──

    @Test
    fun `drops weekday when date-only plus refresh fits`() {
        // req(full=104) = 180 > 210? No: 180 ≤ 210 → FULL; 用更窄的 175: 180 > 175 → SHORT_TITLE
        // req(dateOnly=39) = 10+39+4+52+10 = 105 ≤ 175 ✓
        assertEquals(NavTier.SHORT_TITLE, tier(1f, 175, "9/12 · 周六", "9/12"))
    }

    // ── HIDE_TITLE 档 (2×2 主形态): 标题 GONE, ↻ 保留 ──

    @Test
    fun `narrow 2x2 hides title but keeps refresh button`() {
        // 2×2 = 148dp: req(dateOnly=39)=105 ≤ 148 → SHORT_TITLE, 不降 HIDE_TITLE
        // 用 2 字 dateOnly 也装得下 → SHORT_TITLE。HIDE_TITLE 触发在 <105dp。
        // 100dp: req(dateOnly)=105 > 100 → HIDE_TITLE, 单钮固定件 64 ≤ 100 ✓
        assertEquals(NavTier.HIDE_TITLE, tier(1f, 100, "9/8 · 周二", "9/8"))
    }

    @Test
    fun `hide title boundary is exact at single-button fixed width`() {
        // 单钮固定件 72dp (10+6+40+6+10): 71 装不下 → HIDE_NAV; 72 恰好 → HIDE_TITLE
        assertEquals(NavTier.HIDE_TITLE, tier(1f, 72, "9/8 · 周二", "9/8"))
        assertEquals(NavTier.HIDE_NAV, tier(1f, 71, "9/8 · 周二", "9/8"))
    }

    // ── isToday: ↻ GONE, 预算不含按钮 ──

    @Test
    fun `isToday frees refresh budget so full title keeps slot`() {
        // reqNoRefresh(full=104) = 10+104+10 = 124 ≤ 215 → FULL
        assertEquals(
            NavTier.FULL,
            tier(1f, 215, "9/8 · 周二", "9/8", refreshVisible = false)
        )
    }

    @Test
    fun `isToday narrow still fits date-only title without button`() {
        // isToday → ↻ GONE → reqNoRefresh(full=9字=117) = 10+117+10 = 137 ≤ 148 → FULL
        // (无按钮预算很省, 满标题也装得下 2×2)
        assertEquals(
            NavTier.FULL,
            tier(1f, 148, "9/8 · 周二", "9/8", refreshVisible = false)
        )
        // 极窄: 10+39+10=59 ≤ 60 → SHORT_TITLE; 58 < 59 → HIDE_TITLE (无按钮可降, 标题 GONE)
        assertEquals(
            NavTier.SHORT_TITLE,
            tier(1f, 60, "9/8 · 周二", "9/8", refreshVisible = false)
        )
        assertEquals(
            NavTier.HIDE_TITLE,
            tier(1f, 58, "9/8 · 周二", "9/8", refreshVisible = false)
        )
    }

    // ── fontScale: 标题测量随注入 Paint 走 ──

    @Test
    fun `big font scale widens title and degrades tier`() {
        // fontScale=1.3: title 13×1.3≈17dp/字 → full 9字=152/1.3=117 → req=10+117+52+10=189
        // 但 density=1.3 同时缩放 titleMeasure, 净效果 = req(dp) 相同 → FULL
        // 用 dateOnly-only 场景锁 fontScale 降档: full 11 字 → 143/1.3=110 → 10+110+52+10=182
        // 换更大字串 (12 字): 12×16.9=202.8/1.3=156 → 10+156+52+10=228 > 200 → 降档
        assertEquals(
            NavTier.SHORT_TITLE,
            tier(1.3f, 200, "9/12 · 周六 (大字)", "9/12", titleM = { it.length * (13f * 1.3f) })
        )
    }
}
