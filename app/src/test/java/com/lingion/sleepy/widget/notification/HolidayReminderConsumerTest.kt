package com.lingion.sleepy.widget.notification

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HolidayReminderConsumerTest {
    private fun source(): String = File(
        System.getProperty("sleepy.test.root") ?: ".",
        "app/src/main/java/com/lingion/sleepy/widget/notification/CourseNotificationScheduler.kt"
    ).readText()

    @Test
    fun before_class_window_uses_policy_and_keeps_effective_weekday() {
        val text = source()
        assertTrue(text.contains("dataSource.allowsReminder"))
        assertTrue(text.contains("effectiveDayOfWeek(table.id, date)"))
    }

    @Test
    fun daily_summary_returns_before_building_empty_notification() {
        val text = source()
        val policy = text.indexOf("HolidayReminderPolicyAdapter")
        val returnIndex = text.indexOf(") return", policy)
        assertTrue(policy >= 0)
        assertTrue(returnIndex > policy)
        assertTrue(text.contains("sendScheduleSummary"))
    }

    @Test
    fun all_four_consumers_use_the_shared_policy_without_legacy_cache_checks() {
        val course = source()
        val dnd = File(
            System.getProperty("sleepy.test.root") ?: ".",
            "app/src/main/java/com/lingion/sleepy/widget/notification/ClassDndScheduler.kt"
        ).readText()
        assertTrue(course.contains("HolidayReminderPolicyAdapter"))
        assertTrue(course.contains("dataSource.allowsReminder(table.id, date)"))
        assertTrue(course.contains("decide(context.applicationContext, targetDate, table.id)"))
        assertTrue(course.contains("resolvedTableId"))
        assertTrue(dnd.contains("HolidayReminderPolicyAdapter"))
        assertTrue(!course.contains("isPublicHolidayCached(context.applicationContext, targetDate"))
        assertTrue(!course.contains("isPublicHolidayCached(app, today"))
    }

    @Test
    fun unavailable_policy_data_is_allowed_conservatively() {
        val policy = File(
            System.getProperty("sleepy.test.root") ?: ".",
            "app/src/main/java/com/lingion/sleepy/util/HolidayReminderPolicy.kt"
        ).readText()
        assertTrue(policy.contains("if (!dataAvailable)"))
        assertTrue(policy.contains("allowReminder = true"))
    }

    @Test
    fun before_class_data_source_keeps_legacy_fake_compatibility() {
        val course = source()
        val dataSource = course.substringAfter("internal interface BeforeClassDataSource {")
            .substringBefore("\n}")
        val androidDataSource = course.substringAfter("internal class AndroidBeforeClassDataSource")
            .substringBefore("internal class AndroidBeforeClassAlarmPort")

        assertTrue(dataSource.contains("fun isPublicHoliday(tableId: Long, date: LocalDate): Boolean"))
        assertTrue(dataSource.contains("suspend fun allowsReminder(tableId: Long, date: LocalDate): Boolean = !isPublicHoliday(tableId, date)"))
        assertTrue(androidDataSource.contains("override suspend fun allowsReminder"))
        assertTrue(androidDataSource.contains("HolidayReminderPolicyAdapter"))
    }

    @Test
    fun stale_before_class_alarm_is_suppressed_without_a_resolved_table() {
        val receiver = source().substringAfter("class BeforeClassNotifyReceiver")
            .substringBefore("\n    private fun handle")
        val unresolvedGuard = receiver.indexOf(
            "com.lingion.sleepy.widget.WidgetTableResolver.resolveCurrentTable()?.id\n                    ?: return@launch"
        )
        val handleCall = receiver.indexOf("handle(context, intent)")

        assertTrue(unresolvedGuard >= 0)
        assertTrue(handleCall > unresolvedGuard)
    }

    @Test
    fun fluid_cloud_reconciliation_stops_when_no_active_window_can_start() {
        val course = source()
        val reconcile = course.substringAfter("suspend fun reconcileActiveFluidCloud()")
            .substringBefore("suspend fun ensureActiveFluidCloud()")
        val ensure = course.substringAfter("suspend fun ensureActiveFluidCloud(): Boolean")
            .substringBefore("// ==================== Helpers")

        assertTrue(reconcile.contains("if (!ensureActiveFluidCloud())"))
        assertTrue(reconcile.contains("FluidCloudService.requestStop(app)"))
        assertTrue(ensure.contains("val table = resolveCurrentTable() ?: return false"))
        assertTrue(ensure.contains(".allowReminder\n        ) return false"))
        assertTrue(ensure.contains("!= DateUtils.SemesterStatus.IN_RANGE) return false"))
        assertTrue(ensure.contains("} ?: return false"))
        assertTrue(ensure.contains("return true"))
        assertTrue(ensure.contains("catch (t: Throwable)"))
        assertTrue(ensure.contains("return false"))
    }

    @Test
    fun dnd_rebuild_restores_owned_state_when_no_current_class_interval_remains() {
        val dnd = File(
            System.getProperty("sleepy.test.root") ?: ".",
            "app/src/main/java/com/lingion/sleepy/widget/notification/ClassDndScheduler.kt"
        ).readText()
        val rebuild = dnd.substringAfter("suspend fun rebuildAlarms()")
            .substringBefore("/** Reconcile alarms and current DND state")
        val applyDnd = dnd.substringAfter("fun applyDnd(enter: Boolean)")
            .substringBefore("private fun dndPrefs")

        assertTrue(rebuild.contains(
            "if (isCurrentlyInClass(intervals, now)) applyDnd(enter = true)\n        else applyDnd(enter = false)"
        ))
        assertTrue(applyDnd.contains("isDndOwned(context)"))
        assertTrue(applyDnd.contains("currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY"))
    }

}
