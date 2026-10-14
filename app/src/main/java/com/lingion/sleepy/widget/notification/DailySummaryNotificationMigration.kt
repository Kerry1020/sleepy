package com.lingion.sleepy.widget.notification

import android.app.NotificationManager
import android.content.Context
import android.util.Log

internal data class DailySummaryNotificationIdentity(val channelId: String?, val id: Int, val tag: String?)

/** Channel ownership is required: legacy IDs can collide with unrelated course notifications. */
internal fun legacyDailySummaryNotifications(
    notifications: List<DailySummaryNotificationIdentity>
): List<DailySummaryNotificationIdentity> = notifications.filter {
    it.channelId == CourseNotificationScheduler.CHANNEL_DAILY &&
        it.id != CourseNotificationScheduler.NOTIFY_DAILY &&
        it.id != CourseNotificationScheduler.NOTIFY_TOMORROW_DAILY
}

internal fun cleanupLegacyDailySummaryNotifications(context: Context) {
    runCatching {
        val manager = context.getSystemService(NotificationManager::class.java)
        legacyDailySummaryNotifications(manager.activeNotifications.map {
            DailySummaryNotificationIdentity(it.notification.channelId, it.id, it.tag)
        }).forEach { manager.cancel(it.tag, it.id) }
    }.onFailure { Log.w("DailySummaryMigration", "Unable to remove legacy daily summaries", it) }
}
