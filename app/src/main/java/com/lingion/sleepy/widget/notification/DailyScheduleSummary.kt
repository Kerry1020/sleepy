package com.lingion.sleepy.widget.notification

import com.lingion.sleepy.data.entity.CourseEntity
import com.lingion.sleepy.data.entity.TimeTableEntity
import com.lingion.sleepy.util.TimeTableUtils
import java.time.LocalTime

internal data class DailyTableCourses(val table: TimeTableEntity, val courses: List<CourseEntity>)

internal data class DailyScheduleEntry(
    val table: TimeTableEntity,
    val course: CourseEntity,
    val startTime: LocalTime?,
    val startTimeText: String
)

internal data class DailyScheduleSummary(
    val entries: List<DailyScheduleEntry>,
    val isMultiTable: Boolean
) {
    fun compactText(unknownRoom: String): String = entries.joinToString("\n") { entry ->
        val source = if (isMultiTable) "[${entry.table.name}] " else ""
        val room = entry.course.room.ifBlank { unknownRoom }
        "$source${entry.startTimeText} ${entry.course.courseName} @ $room"
    }
}

/** Call after reminder, semester and holiday filtering; tables must have hydrated timeJson. */
internal fun buildDailyScheduleSummary(tables: List<DailyTableCourses>): DailyScheduleSummary {
    val entries = tables.flatMap { (table, courses) ->
        val nodeTimes = TimeTableUtils.parseNodes(table.timeJson).associateBy { it.node }
        courses.map { course ->
            val customTime = course.startTime.takeIf { course.ownTime && it.isNotBlank() }
            val start = if (customTime != null) {
                runCatching { LocalTime.parse(customTime) }.getOrNull()
            } else {
                nodeTimes[course.startNode]?.start
            }
            val text = customTime ?: start?.let { "%02d:%02d".format(it.hour, it.minute) }.orEmpty()
            DailyScheduleEntry(table, course, start, text)
        }
    }.sortedWith(compareBy<DailyScheduleEntry> { it.startTime == null }.thenBy { it.startTime })
    // Empty tables still count as sources: tomorrow's classes may be from a different table.
    return DailyScheduleSummary(entries, isMultiTable = tables.size > 1)
}
