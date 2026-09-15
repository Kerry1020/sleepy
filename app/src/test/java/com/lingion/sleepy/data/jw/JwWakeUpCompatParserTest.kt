package com.lingion.sleepy.data.jw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JwWakeUpCompatParserTest {
    @Test
    fun `legacy chaoxing uses kckbData schema`() {
        val source = """
            {"data":{"kckbData":[{"kcmc":"<b>高等数学</b>","xq":"2","djc":3,"tmc":"张老师","croommc":"教一-101","zc":"1-16,18","zctype":"0"}]}}
        """.trimIndent()

        val courses = JwChaoxingLegacyParser(source).generateCourseList()

        assertEquals(2, courses.size)
        assertEquals("高等数学", courses[0].name)
        assertEquals(2, courses[0].day)
        assertEquals(3, courses[0].startNode)
        assertEquals(16, courses[0].endWeek)
        assertEquals(18, courses[1].startWeek)
    }

    @Test
    fun `cumtb joins lesson names to schedule rows and converts time`() {
        val source = """
            {"result":{"lessonList":[{"id":7,"courseName":"数据库"}],"scheduleList":[{"lessonId":7,"personName":"李老师","weekday":4,"weekIndex":6,"startTime":800,"endTime":945,"room":{"nameZh":"信科-201"}}]}}
        """.trimIndent()

        val courses = JwCumtbParser(source).generateCourseList()

        assertEquals(1, courses.size)
        assertEquals("数据库", courses.single().name)
        assertEquals(4, courses.single().day)
        assertEquals(1, courses.single().startNode)
        assertEquals(2, courses.single().endNode)
        assertEquals("信科-201", courses.single().room)
        assertEquals(6, courses.single().startWeek)
    }

    @Test
    fun `south soft parses xq key and odd-even week tokens`() {
        val source = """
            [{"KCWZSM":"线性代数","KEY":"xq3_jc2","SKSJ":"[数学][周1-16单][张老师][A-101][第2-3节]"}]
        """.trimIndent()

        val courses = JwSouthSoftParser(source).generateCourseList()

        assertTrue(courses.isNotEmpty())
        assertEquals("线性代数", courses.single().name)
        assertEquals(3, courses.single().day)
        assertEquals(2, courses.single().startNode)
        assertEquals(3, courses.single().endNode)
        assertEquals(1, courses.single().type)
        assertEquals(16, courses.single().endWeek)
    }

    @Test
    fun `shuwei activities preserve odd even week ranges`() {
        val source = """
            {"activities":[{"courseName":"编译原理","weekday":5,"startSection":7,"endSection":8,"weeksStr":"1-15单,2-16双","room":"C-201","teacher":"王老师"}]}
        """.trimIndent()

        val courses = JwShuweiParser(source).generateCourseList()

        assertEquals(2, courses.size)
        assertEquals(1, courses[0].type)
        assertEquals(15, courses[0].endWeek)
        assertEquals(2, courses[1].type)
        assertEquals(16, courses[1].endWeek)
    }
}
