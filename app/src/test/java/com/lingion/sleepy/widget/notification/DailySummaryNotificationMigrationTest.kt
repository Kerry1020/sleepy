package com.lingion.sleepy.widget.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryNotificationMigrationTest {
    @Test
    fun onlyRetiredDailyChannelNotificationsAreRemovedWithTheirTags() {
        val legacy = DailySummaryNotificationIdentity("sleepy_daily", 5002, "table-1")
        val collision = DailySummaryNotificationIdentity("sleepy_before_class", 5002, "course-3002")
        val currentToday = DailySummaryNotificationIdentity("sleepy_daily", 1001, null)
        val currentTomorrow = DailySummaryNotificationIdentity("sleepy_daily", 1002, null)
        val fluid = DailySummaryNotificationIdentity("sleepy_fluid_v2", 2000, null)
        val unrelated = DailySummaryNotificationIdentity(null, 9000, null)
        assertEquals(listOf(legacy), legacyDailySummaryNotifications(listOf(
            legacy, collision, currentToday, currentTomorrow, fluid, unrelated
        )))
    }

    @Test
    fun channelOwnershipHandlesLegacyIntegerOverflowWithoutIdRangeAssumptions() {
        val overflowedId = DailySummaryNotificationIdentity("sleepy_daily", -5000, null)
        assertEquals(listOf(overflowedId), legacyDailySummaryNotifications(listOf(overflowedId)))
    }
}
