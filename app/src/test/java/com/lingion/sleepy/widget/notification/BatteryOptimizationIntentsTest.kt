package com.lingion.sleepy.widget.notification

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryOptimizationIntentsTest {
    @Test
    fun `vendor power pages precede standard battery dialogs`() {
        LiveCardVendor.entries.forEach { vendor ->
            val specs = vendorBatterySpecs(vendor)
            assertTrue(specs.isNotEmpty())
            if (vendor != LiveCardVendor.MEIZU && vendor != LiveCardVendor.SAMSUNG && vendor != LiveCardVendor.GENERIC) {
                // 厂商页必须排第一 — 原生列表页没有"无限制后台活动"选项
                assertEquals("vendor page first for $vendor", false, specs.first().component == null)
            }
            val actions = specs.mapNotNull { it.action }
            assertEquals(
                "request dialog precedes native list for $vendor",
                listOf(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                ),
                actions.takeLast(2),
            )
        }
    }

    @Test
    fun `generic vendors use only standard battery entries`() {
        LiveCardVendor.entries
            .filter { it in setOf(LiveCardVendor.MEIZU, LiveCardVendor.SAMSUNG, LiveCardVendor.GENERIC) }
            .forEach { vendor ->
                assertEquals(2, vendorBatterySpecs(vendor).size)
            }
    }

    @Test
    fun `xiaomi targets the powerkeeper hidden apps config page`() {
        val first = vendorBatterySpecs(LiveCardVendor.XIAOMI).first()
        assertEquals("com.miui.powerkeeper/com.miui.powerkeeper.ui.HiddenAppsConfigActivity", first.component)
    }
}
