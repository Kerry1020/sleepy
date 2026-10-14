package com.lingion.sleepy.widget.notification

import com.lingion.sleepy.data.entity.CourseEntity
import com.lingion.sleepy.data.entity.TimeTableEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DailyScheduleSummaryTest {
    private fun table(id: Long, name: String, first: String = "08:00") = TimeTableEntity(
        id = id, name = name, startDate = "2026-09-01",
        timeJson = """[{"node":1,"start":"$first","end":"10:00"},{"node":2,"start":"14:00","end":"15:00"}]"""
    )

    private fun course(id: Long, tableId: Long, node: Int, custom: String? = null) = CourseEntity(
        id = id, tableId = tableId, groupId = "g$id", courseName = "课$id", room = "101",
        day = 1, startNode = node, step = 1, startWeek = 1, endWeek = 20, color = "#FF6750A4",
        ownTime = custom != null, startTime = custom.orEmpty()
    )

    @Test
    fun interleavedTablesAreSortedByActualTimeRatherThanTableOrNode() {
        val summary = buildDailyScheduleSummary(listOf(
            DailyTableCourses(table(1, "主课表"), listOf(course(1, 1, 1), course(3, 1, 2))),
            DailyTableCourses(table(2, "辅修", "09:00"), listOf(course(2, 2, 1)))
        ))
        assertEquals(listOf(1L, 2L, 3L), summary.entries.map { it.course.id })
        assertEquals("[主课表] 08:00 课1 @ 101\n[辅修] 09:00 课2 @ 101\n[主课表] 14:00 课3 @ 101",
            summary.compactText("未知教室"))
    }

    @Test
    fun sourceIsPreservedWhenOnlyOneOfMultipleTablesHasCourses() {
        val summary = buildDailyScheduleSummary(listOf(
            DailyTableCourses(table(1, "主课表"), emptyList()),
            DailyTableCourses(table(2, "辅修"), listOf(course(2, 2, 1).copy(room = "")))
        ))
        assertTrue(summary.isMultiTable)
        assertEquals("[辅修] 08:00 课2 @ 未知教室", summary.compactText("未知教室"))
    }

    @Test
    fun singleTableStillUsesSingleTableFormatAndChoosesActualFirstCourse() {
        val summary = buildDailyScheduleSummary(listOf(
            DailyTableCourses(table(1, "主课表"), listOf(course(2, 1, 2), course(1, 1, 1)))
        ))
        assertFalse(summary.isMultiTable)
        assertEquals(1L, summary.entries.first().course.id)
        assertEquals("08:00", summary.entries.first().startTimeText)
        assertEquals("08:00 课1 @ 101\n14:00 课2 @ 101", summary.compactText("未知教室"))
    }

    @Test
    fun customStartOverridesNodeAndMissingTimesAreKeptAtTheEnd() {
        val summary = buildDailyScheduleSummary(listOf(DailyTableCourses(table(1, "主课表"), listOf(
            course(1, 1, 99), course(2, 1, 1), course(3, 1, 2, "07:30"),
            course(4, 1, 1, "bad-time"), course(5, 1, 99, "23:59")
        ))))
        assertEquals(listOf(3L, 2L, 5L, 1L, 4L), summary.entries.map { it.course.id })
        assertEquals(LocalTime.of(7, 30), summary.entries.first().startTime)
        assertEquals("", summary.entries[3].startTimeText)
        assertEquals("bad-time", summary.entries[4].startTimeText)
    }

    @Test
    fun invalidTimeTableDoesNotHideCoursesOrPreventCustomTimesFromSorting() {
        val summary = buildDailyScheduleSummary(listOf(DailyTableCourses(
            table(1, "主课表").copy(timeJson = "invalid"),
            listOf(course(1, 1, 1), course(2, 1, 2, "09:15"))
        )))
        assertEquals(listOf(2L, 1L), summary.entries.map { it.course.id })
        assertEquals(2, summary.entries.size)
    }

    @Test
    fun noCoursesReturnsEmptySummaryWithSourceContext() {
        val summary = buildDailyScheduleSummary(listOf(
            DailyTableCourses(table(1, "主课表"), emptyList()),
            DailyTableCourses(table(2, "辅修"), emptyList())
        ))
        assertTrue(summary.entries.isEmpty())
        assertTrue(summary.isMultiTable)
        assertEquals("", summary.compactText("未知教室"))
    }
}
