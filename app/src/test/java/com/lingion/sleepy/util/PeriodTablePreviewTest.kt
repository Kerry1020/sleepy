package com.lingion.sleepy.util

import com.lingion.sleepy.data.entity.CourseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * issue#40 设计 §5.2/§5.3 纯函数预览契约:
 * 新旧两份 timeJson 对同一批节次编号解析出 旧时间→新时间;
 * 自定义时间(ownTime)课程不参与解释(设计 §2-7);
 * 节次编号永远不被改写(§9.1 节次绑定不重算)。
 */
class PeriodTablePreviewTest {

    private val oldJson = """
        [{"node":1,"start":"08:00","end":"08:45"},
         {"node":2,"start":"08:55","end":"09:40"},
         {"node":3,"start":"10:00","end":"10:45"},
         {"node":4,"start":"10:55","end":"11:40"}]
    """.trimIndent()

    /** 第一节后移 20 分钟, 其余不变 */
    private val newJson = """
        [{"node":1,"start":"08:20","end":"09:05"},
         {"node":2,"start":"08:55","end":"09:40"},
         {"node":3,"start":"10:00","end":"10:45"},
         {"node":4,"start":"10:55","end":"11:40"}]
    """.trimIndent()

    private fun course(
        name: String,
        startNode: Int = 1,
        step: Int = 2,
        ownTime: Boolean = false
    ) = CourseEntity(
        id = 0,
        groupId = "g-$name",
        tableId = 1,
        courseName = name,
        day = 1,
        startNode = startNode,
        step = step,
        startWeek = 1,
        endWeek = 16,
        color = "#FF6750A4",
        ownTime = ownTime,
        startTime = if (ownTime) "07:30" else "",
        endTime = if (ownTime) "09:00" else ""
    )

    @Test
    fun changed_node_shows_old_to_new_time_for_bound_courses() {
        val courses = listOf(course("高数", startNode = 1, step = 2))
        val preview = TimeTableUtils.previewPeriodTableChange(oldJson, newJson, courses)
        assertEquals(1, preview.changedCourses.size)
        val change = preview.changedCourses.first()
        assertEquals("高数", change.courseName)
        assertEquals("08:00-09:40", change.oldTime)
        assertEquals("08:20-09:40", change.newTime)
        assertEquals(0, preview.unchangedCount)
    }

    @Test
    fun course_on_untouched_nodes_counts_as_unchanged() {
        val courses = listOf(
            course("高数", startNode = 1, step = 2),
            course("英语", startNode = 3, step = 2)
        )
        val preview = TimeTableUtils.previewPeriodTableChange(oldJson, newJson, courses)
        assertEquals(1, preview.changedCourses.size)
        assertEquals("高数", preview.changedCourses.first().courseName)
        assertEquals(1, preview.unchangedCount)
    }

    @Test
    fun ownTime_courses_are_excluded_from_interpretation() {
        val courses = listOf(
            course("自定义课", startNode = 1, step = 2, ownTime = true),
            course("高数", startNode = 1, step = 1)
        )
        val preview = TimeTableUtils.previewPeriodTableChange(oldJson, newJson, courses)
        assertEquals(1, preview.changedCourses.size)
        assertTrue(preview.changedCourses.none { it.courseName == "自定义课" })
        // 自定义课不计入"无变化"——它根本不参与解释
        assertEquals(0, preview.unchangedCount)
    }

    @Test
    fun identical_timejson_yields_no_changes() {
        val courses = listOf(course("高数"), course("英语", startNode = 3, step = 1))
        val preview = TimeTableUtils.previewPeriodTableChange(oldJson, oldJson, courses)
        assertTrue(preview.changedCourses.isEmpty())
        assertEquals(2, preview.unchangedCount)
    }

    @Test
    fun missing_node_in_old_table_reports_null_old_time() {
        val sparseOld = """[{"node":1,"start":"08:00","end":"08:45"}]"""
        val courses = listOf(course("大四专选", startNode = 3, step = 1))
        val preview = TimeTableUtils.previewPeriodTableChange(sparseOld, newJson, courses)
        assertEquals(1, preview.changedCourses.size)
        assertNull(preview.changedCourses.first().oldTime)
        assertEquals("10:00-10:45", preview.changedCourses.first().newTime)
    }

    @Test
    fun node_numbers_are_never_rewritten_by_preview() {
        // §9.1: 节次绑定不重算 — 预览产物里 startNode/step 必须原样
        // (第 2-4 节新旧一致, 故用整批跨节次的课让它落在变化区间里)
        val courses = listOf(course("高数", startNode = 1, step = 3))
        val preview = TimeTableUtils.previewPeriodTableChange(oldJson, newJson, courses)
        val change = preview.changedCourses.firstOrNull()
            ?: error("跨变化节次的课必须出现在预览里")
        assertEquals(1, change.startNode)
        assertEquals(3, change.step)
    }

    @Test
    fun empty_courses_give_empty_preview() {
        val preview = TimeTableUtils.previewPeriodTableChange(oldJson, newJson, emptyList())
        assertTrue(preview.changedCourses.isEmpty())
        assertEquals(0, preview.unchangedCount)
    }
}
