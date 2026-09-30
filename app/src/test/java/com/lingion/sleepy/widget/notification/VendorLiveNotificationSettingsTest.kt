package com.lingion.sleepy.widget.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class VendorLiveNotificationSettingsTest {
    @Test
    fun `settings order is channel then vendor then application`() {
        LiveCardVendor.entries.forEach { vendor ->
            val specs = vendorSettingsSpecs(vendor)
            assertEquals(ACTION_CHANNEL_NOTIFICATION_SETTINGS, specs.first().action)
            assertEquals(ACTION_APP_NOTIFICATION_SETTINGS, specs.last().action)
        }
    }

    @Test
    fun `vendor specific page sits between channel and application`() {
        // 通道页直达流体云通道本身, 必须排第一; 厂商页(如 OxygenOS 全局通知管理页)
        // 排其后作为厂商增强; 应用通知页永远兜底。
        val specs = vendorSettingsSpecs(LiveCardVendor.OPPO)
        assertEquals(3, specs.size)
        assertEquals(ACTION_CHANNEL_NOTIFICATION_SETTINGS, specs[0].action)
        assertEquals("com.coloros.notificationmanager.action.NOTIFICATION_SETTINGS", specs[1].action)
        assertEquals(ACTION_APP_NOTIFICATION_SETTINGS, specs[2].action)
    }
}
