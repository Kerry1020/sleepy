package com.lingion.sleepy.widget.notification

import com.lingion.sleepy.data.entity.CourseEntity
import com.lingion.sleepy.data.entity.TimeTableEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 7 天课前闹钟预排纯 JVM 单测 — 走注入端口 (BeforeClassAlarmPort/Env/DataSource),
 * 不依赖 Android 运行时。核心不变量:
 *  ① 窗口内未来的课 → 一节课一颗精确闹钟 (courseId 稳定 requestCode);
 *  ② 重复调用先清扫后重排, 不产生重复槽位;
 *  ③ 学期范围外/已过触发点/开关关闭 → 零闹钟。
 */
class CourseNotificationSchedulerTest {

    private val fixedToday: LocalDate = LocalDate.of(2026, 10, 7) // 星期三
    private val zone get() = ZoneId.systemDefault()

    private class FakeAlarmPort : BeforeClassAlarmPort {
        val armed = LinkedHashMap<Int, Long>()   // requestCode → epochMs (重复 set 覆盖, 语义同 PendingIntent)
        val cancels = mutableListOf<Int>()
        var setCount = 0
        override fun setExact(requestCode: Int, epochMs: Long, extras: Map<String, Any?>) {
            setCount++
            armed[requestCode] = epochMs
        }
        override fun cancel(requestCode: Int) {
            cancels += requestCode
            armed.remove(requestCode)
        }
    }

    private class FakeEnv(
        private val enabled: Boolean = true,
        private val minutes: Int = 10,
        private val now: Long = System.currentTimeMillis(),
        private val today: LocalDate = LocalDate.of(2026, 10, 7),
    ) : BeforeClassEnv {
        override fun isBeforeClassEnabled() = enabled
        override fun beforeClassMinutes() = minutes
        override fun nowEpochMs() = now
        override fun todayDate() = today
    }

    private class FakeDataSource(
        private val table: TimeTableEntity?,
        private val byDay: Map<Int, List<CourseEntity>>,
    ) : BeforeClassDataSource {
        override suspend fun resolveCurrentTable() = table
        override suspend fun coursesForDay(tableId: Long, dayOfWeek: Int) = byDay[dayOfWeek].orEmpty()
        override suspend fun allCourseIds() = byDay.values.flatten().map { it.id }
        override fun effectiveDayOfWeek(tableId: Long?, date: LocalDate) = date.dayOfWeek.value
    }

    private fun table(
        startDate: LocalDate = fixedToday.minusDays(7),
        maxWeek: Int = 20,
    ) = TimeTableEntity(id = 1L, name = "t", startDate = startDate.toString(), maxWeek = maxWeek)

    private fun course(
        id: Long,
        day: Int,
        startNode: Int,
        ownTime: Boolean = false,
        startTime: String = "",
    ) = CourseEntity(
        id = id, groupId = "g$id", tableId = 1L,
        courseName = "课$id", teacher = "师", room = "101",
        day = day, startNode = startNode, step = 1,
        startWeek = 1, endWeek = 20, type = 0,
        color = "#FF6750A4",
        ownTime = ownTime, startTime = startTime, endTime = "",
    )

    private fun scheduler(
        port: FakeAlarmPort,
        env: BeforeClassEnv,
        src: FakeDataSource,
    ) = CourseNotificationScheduler(port, env, src)

    @Test
    fun `窗口内未来的课 排一颗精确闹钟 epoch=开课减提前分钟`() = runBlocking {
        // 周三今天, 周五(day=5)第1节 08:00 → 2026-10-09 07:50
        val src = FakeDataSource(table(), mapOf(5 to listOf(course(7, day = 5, startNode = 1))))
        val port = FakeAlarmPort()
        val now = fixedToday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        scheduler(port, FakeEnv(now = now), src).scheduleNext7DaysExactAlarms()

        val expected = fixedToday.plusDays(2).atTime(7, 50).atZone(zone).toInstant().toEpochMilli()
        assertEquals(mapOf(100 + 7 to expected), port.armed)
    }

    @Test
    fun `重复调用幂等 第二次先清扫再重排 槽位数不增长`() = runBlocking {
        val src = FakeDataSource(table(), mapOf(5 to listOf(course(7, day = 5, startNode = 1))))
        val port = FakeAlarmPort()
        val now = fixedToday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val s = scheduler(port, FakeEnv(now = now), src)
        s.scheduleNext7DaysExactAlarms()
        val firstArmed = port.armed.toMap()
        val firstSetCount = port.setCount
        port.cancels.clear(); port.setCount = 0

        s.scheduleNext7DaysExactAlarms()
        assertEquals("第二次调用前先清扫了全部旧槽", listOf(100 + 7), port.cancels)
        assertEquals("重排后槽位数不变", firstArmed, port.armed)
        assertEquals(firstSetCount, port.setCount)
    }

    @Test
    fun `学期范围外 零闹钟`() = runBlocking {
        // 学期 30 天后才开始 → 窗口 7 天全在学期外
        val src = FakeDataSource(table(startDate = fixedToday.plusDays(30)), mapOf(5 to listOf(course(7, day = 5, startNode = 1))))
        val port = FakeAlarmPort()
        scheduler(port, FakeEnv(now = fixedToday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()), src)
            .scheduleNext7DaysExactAlarms()
        assertTrue(port.armed.isEmpty())
    }

    @Test
    fun `已过触发点跳过 未来的同名周课不受影响`() = runBlocking {
        // 周三今天(day=3) 08:00 的课, 现在 09:00 → 今日已过点, 跳过
        // 若配置成未来日仍要排: day=5 周五同 id 结构体不同课程
        val src = FakeDataSource(table(), mapOf(
            3 to listOf(course(3, day = 3, startNode = 1)),
            5 to listOf(course(5, day = 5, startNode = 1)),
        ))
        val port = FakeAlarmPort()
        scheduler(port, FakeEnv(now = fixedToday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()), src)
            .scheduleNext7DaysExactAlarms()
        assertEquals(listOf(100 + 5), port.armed.keys.toList())
    }

    @Test
    fun `开关关闭 零闹钟`() = runBlocking {
        val src = FakeDataSource(table(), mapOf(5 to listOf(course(7, day = 5, startNode = 1))))
        val port = FakeAlarmPort()
        scheduler(port, FakeEnv(enabled = false), src).scheduleNext7DaysExactAlarms()
        assertTrue(port.armed.isEmpty())
    }

    @Test
    fun `ownTime 课用自定义开始时间`() = runBlocking {
        val src = FakeDataSource(table(), mapOf(5 to listOf(course(9, day = 5, startNode = 1, ownTime = true, startTime = "15:30"))))
        val port = FakeAlarmPort()
        scheduler(port, FakeEnv(now = fixedToday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()), src)
            .scheduleNext7DaysExactAlarms()
        val expected = fixedToday.plusDays(2).atTime(15, 20).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, port.armed[100 + 9])
    }

    @Test
    fun `破损时间 非法时 跳过不崩`() = runBlocking {
        val src = FakeDataSource(table(), mapOf(5 to listOf(course(9, day = 5, startNode = 1, ownTime = true, startTime = "25:99"))))
        val port = FakeAlarmPort()
        scheduler(port, FakeEnv(now = fixedToday.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()), src)
            .scheduleNext7DaysExactAlarms()
        assertTrue(port.armed.isEmpty())
    }
}
