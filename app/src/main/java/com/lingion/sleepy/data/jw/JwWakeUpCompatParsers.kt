package com.lingion.sleepy.data.jw

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.Locale

/** Compatibility parsers for WakeUp protocol variants. */
private object WakeUpCompat {
    fun parse(source: String, markers: Set<String>, jsonHint: Boolean = false): List<JwCourse> {
        parseJson(source)?.let { parseJsonCourses(it).takeIf { courses -> courses.isNotEmpty() }?.let { return it } }
        parseMarkedTable(source, markers).takeIf { it.isNotEmpty() }?.let { return it }
        return if (jsonHint) emptyList() else parseDelimited(source)
    }

    fun parseChaoxing(source: String): List<JwCourse> {
        val root = parseJson(source) as? JSONObject ?: return emptyList()
        val rows = root.optJSONObject("data")?.optJSONArray("kckbData") ?: root.optJSONArray("kckbData") ?: return emptyList()
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val name = stripMarkup(row.optString("kcmc"))
                val day = row.optString("xq").toIntOrNull() ?: continue
                val node = row.optInt("djc", 0)
                if (name.isBlank() || day !in 1..7 || node < 1) continue
                val type = row.optString("zctype").toIntOrNull() ?: 0
                row.optString("zc").split(',').forEach { token ->
                    val weeks = Regex("\\d+").findAll(token).map { it.value.toInt() }.toList()
                    if (weeks.isNotEmpty()) add(JwCourse(name, stripMarkup(row.optString("croommc")), stripMarkup(row.optString("tmc")), day, node, node, weeks.first(), weeks.getOrNull(1) ?: weeks.first(), type))
                }
            }
        }
    }

    fun parseCumtb(source: String): List<JwCourse> {
        val payload = (parseJson(source) as? JSONObject)?.optJSONObject("result") ?: return emptyList()
        val names = mutableMapOf<Int, String>()
        val lessons = payload.optJSONArray("lessonList") ?: JSONArray()
        for (i in 0 until lessons.length()) lessons.optJSONObject(i)?.let { names[it.optInt("id", -1)] = it.optString("courseName") }
        val schedules = payload.optJSONArray("scheduleList") ?: return emptyList()
        return buildList {
            for (i in 0 until schedules.length()) {
                val row = schedules.optJSONObject(i) ?: continue
                val name = names[row.optInt("lessonId", -1)].orEmpty()
                val day = row.optInt("weekday", 0)
                val week = row.optInt("weekIndex", 0)
                val startTime = row.optInt("startTime", 0)
                val start = timeToNode(startTime)
                if (name.isBlank() || day !in 1..7 || week < 1 || start < 1) continue
                val count = durationToNodes(startTime, row.optInt("endTime", 0))
                add(JwCourse(name, row.optJSONObject("room")?.optString("nameZh").orEmpty(), row.optString("personName"), day, start, start + count - 1, week, week))
            }
        }
    }

    fun parseSouthSoft(source: String): List<JwCourse> {
        val root = parseJson(source)
        val rows = when (root) {
            is JSONArray -> root
            is JSONObject -> root.optJSONArray("value") ?: root.optJSONArray("data") ?: return emptyList()
            else -> return emptyList()
        }
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (row.optString("KEY") == "bz") continue
                val day = Regex("xq(\\d+)_jc\\d+").find(row.optString("KEY"))?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val fields = Regex("\\[(.*?)\\]").findAll(row.optString("SKSJ")).map { it.groupValues[1].trim() }.toList()
                if (fields.size < 5) continue
                val nodes = fields[4].replace("第", "").replace("节", "").split('-')
                val start = nodes.firstOrNull()?.toIntOrNull() ?: continue
                val end = nodes.getOrNull(1)?.toIntOrNull() ?: start
                fields[1].split(',').forEach { token ->
                    val weeks = Regex("\\d+").findAll(token).map { it.value.toInt() }.toList()
                    if (weeks.isNotEmpty()) add(JwCourse(row.optString("KCWZSM").ifBlank { fields[0] }, fields[4], fields[2], day, start, end, weeks.first(), weeks.getOrNull(1) ?: weeks.first(), when { token.contains("单") -> 1; token.contains("双") -> 2; else -> 0 }))
                }
            }
        }
    }

    fun confidence(source: String, markers: Set<String>): Int = when {
        markers.any { source.contains(it, ignoreCase = true) } -> 90
        source.trimStart().startsWith("{") || source.trimStart().startsWith("[") -> 70
        else -> 0
    }

    fun features(source: String, markers: Set<String>): List<String> = markers.filter { source.contains(it, true) }.map { "marker=$it" }

    fun parseShuwei(source: String): List<JwCourse> {
        val root = parseJson(source) ?: return emptyList()
        val result = mutableListOf<JwCourse>()
        fun visit(value: Any) {
            when (value) {
                is JSONArray -> for (i in 0 until value.length()) value.opt(i)?.let(::visit)
                is JSONObject -> {
                    listOf("activities", "courseUnits", "unitList").forEach { key ->
                        value.optJSONArray(key)?.let { array ->
                            for (i in 0 until array.length()) {
                                val row = array.optJSONObject(i) ?: continue
                                parseActivity(row)?.let(result::addAll)
                            }
                        }
                    }
                    for (key in value.keys()) value.opt(key)?.let(::visit)
                }
            }
        }
        visit(root)
        return result.distinctBy { listOf(it.name, it.day, it.startNode, it.startWeek, it.endWeek, it.type) }
    }

    private fun parseActivity(row: JSONObject): List<JwCourse>? {
        val name = first(row, "courseName", "course", "name", "kcmc")
        val day = firstInt(row, "weekday", "weekDay", "dayOfWeek", "classDay", "xqj", "day")
        val start = firstInt(row, "startSection", "beginSection", "beginNumber", "startNode", "ksjc", "start")
        if (name.isBlank() || day == null || day !in 1..7 || start == null || start < 1) return null

        val end = firstInt(row, "endSection", "endNumber", "endNode", "jsjc", "end") ?: start
        val weeks = parseWeekTokens(first(row, "weeksStr", "weekDescription", "week", "weeks", "zc"))
        if (weeks.isEmpty()) return null
        val room = first(row, "room", "classroom", "classroomName", "location", "JASMC", "cdmc")
        val teacher = first(row, "teacher", "teacherName", "teachers", "attendClassTeacher", "SKJS", "xm")
        return weeks.map { (from, to, type) -> JwCourse(name, room, teacher, day, start, end, from, to, type) }
    }

    private fun parseWeekTokens(value: String): List<Triple<Int, Int, Int>> {
        if (value.isBlank()) return emptyList()
        return value.replace("周", "").split(',', '，', ';', '；').mapNotNull { raw ->
            val token = raw.trim()
            if (token.isBlank()) return@mapNotNull null
            val type = when {
                token.contains("单") -> 1
                token.contains("双") -> 2
                else -> 0
            }
            val numbers = Regex("\\d+").findAll(token).map { it.value.toInt() }.toList()
            if (numbers.isEmpty()) null else Triple(numbers.first(), numbers.getOrNull(1) ?: numbers.first(), type)
        }
    }

    private fun parseJson(source: String): Any? = try {
        when (val text = source.trim()) {
            else -> when {
                text.startsWith("{") -> JSONObject(text)
                text.startsWith("[") -> JSONArray(text)
                else -> null
            }
        }
    } catch (_: Exception) { null }

    private fun parseJsonCourses(root: Any): List<JwCourse> {
        val result = mutableListOf<JwCourse>()
        fun walk(value: Any, depth: Int) {
            if (depth > 5) return
            when (value) {
                is JSONArray -> for (i in 0 until value.length()) value.opt(i)?.let { item ->
                    if (item is JSONObject) {
                        val name = first(item, "courseName", "kcmc", "KCM", "name", "course")
                        val day = firstInt(item, "weekday", "dayOfWeek", "xqj", "day", "classDay", "xq")
                        val node = firstInt(item, "beginNumber", "beginSection", "startNode", "ksjc", "start")
                        if (name.isNotBlank() && day != null && node != null) result += JwCourse(name, first(item, "location", "room", "classroomName", "JASMC", "cdmc"), first(item, "teacherName", "teacher", "teachers", "SKJS", "xm"), day.coerceIn(1, 7), node, firstInt(item, "endNumber", "endSection", "endNode", "jsjc", "end") ?: node, 1, 16)
                    }
                    walk(item, depth + 1)
                }
                is JSONObject -> for (key in value.keys()) value.opt(key)?.let { walk(it, depth + 1) }
            }
        }
        walk(root, 0)
        return result
    }

    private fun parseMarkedTable(source: String, markers: Set<String>): List<JwCourse> = buildList {
        for (row in Jsoup.parse(source).select("tr")) {
            val cells = row.select("td,th")
            if (cells.size < 3) continue
            val text = cells.joinToString(" | ") { it.text().trim() }
            if (markers.isNotEmpty() && markers.none { source.contains(it, true) } && row.attr("data-day").isBlank()) continue
            val name = row.attr("data-course").ifBlank { cells.firstOrNull()?.text()?.trim().orEmpty() }
            val day = row.attr("data-day").toIntOrNull() ?: extractDay(text) ?: continue
            val node = row.attr("data-node").toIntOrNull() ?: extractNode(text) ?: continue
            if (name.isNotBlank()) add(JwCourse(name, row.attr("data-room").ifBlank { cells.getOrNull(3)?.text().orEmpty() }, row.attr("data-teacher").ifBlank { cells.getOrNull(2)?.text().orEmpty() }, day, node, node, 1, 16))
        }
    }

    private fun parseDelimited(source: String): List<JwCourse> = source.lineSequence().mapNotNull { line ->
        val p = line.split(',', '\t').map { it.trim() }
        val day = p.getOrNull(1)?.toIntOrNull()
        val node = p.getOrNull(2)?.toIntOrNull()
        if (p.size >= 5 && day != null && day in 1..7 && node != null) JwCourse(p[0], p.getOrNull(3).orEmpty(), p.getOrNull(4).orEmpty(), day, node, node, 1, 16) else null
    }.toList()

    private fun first(o: JSONObject, vararg keys: String): String = keys.firstNotNullOfOrNull { o.optString(it).takeIf(String::isNotBlank) }.orEmpty()
    private fun firstInt(o: JSONObject, vararg keys: String): Int? = keys.firstNotNullOfOrNull { o.optString(it).filter(Char::isDigit).toIntOrNull() }
    private fun stripMarkup(value: String): String = Jsoup.parse(value).text().trim()
    private fun extractDay(text: String): Int? = Regex("(?:星期|周)\\s*([一二三四五六日天1-7])").find(text)?.groupValues?.get(1)?.let { when (it) { "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5; "六" -> 6; "日", "天" -> 7; else -> it.toIntOrNull() } }
    private fun extractNode(text: String): Int? = Regex("(?:第\\s*)?(\\d{1,2})\\s*(?:[-~至](\\d{1,2}))?\\s*节").find(text)?.groupValues?.get(1)?.toIntOrNull()
    private fun timeToNode(time: Int): Int = when { time < 1230 -> ((time - 800) / 100) + 1; time < 1800 -> ((time - 1400) / 100) + 5; else -> ((time - 1900) / 100) + 9 }
    private fun durationToNodes(start: Int, end: Int): Int = when (end - start) { in 50..99 -> 1; in 100..200 -> 2; in 210..340 -> 3; else -> 4 }
}

abstract class WakeUpMarkerParser(source: String, private val markers: Set<String>) : JwParser(source) {
    override fun generateCourseList(): List<JwCourse> = WakeUpCompat.parse(source, markers)
    override fun confidence(): Int = WakeUpCompat.confidence(source, markers)
    override fun matchedFeatures(): List<String> = WakeUpCompat.features(source, markers)
}
class JwKingoParser(source: String) : WakeUpMarkerParser(source, setOf("kingosoft", "courseTableForStd", "courseTableStudent", "new TaskActivity"))
class JwJzParser(source: String) : WakeUpMarkerParser(source, setOf("courseTableForStd", "courseTableStudent", "wisedu", "JinZhi"))
class JwSouthSoftParser(source: String) : WakeUpMarkerParser(source, setOf("studentTableVms", "studentTableVm", "activities", "south_soft")) {
    override fun generateCourseList() = WakeUpCompat.parseSouthSoft(source).ifEmpty { super.generateCourseList() }
}
class JwChaoxingLegacyParser(source: String) : WakeUpMarkerParser(source, setOf("kckbData", "LessonArray", "queryKbForGrdb", "Powered by ChaoXing")) {
    override fun generateCourseList() = WakeUpCompat.parseChaoxing(source).ifEmpty { super.generateCourseList() }
}
class JwShuweiParser(source: String) : WakeUpMarkerParser(source, setOf("courseUnits", "unitCount", "activities", "wut_table", "shuwei")) {
    override fun generateCourseList() = WakeUpCompat.parseShuwei(source).ifEmpty { super.generateCourseList() }
}
class JwSudaParser(source: String) : WakeUpMarkerParser(source, setOf("print-schedule-table", "default2.aspx", "xskbcx.aspx", "suda"))
class JwCumtbParser(source: String) : WakeUpMarkerParser(source, setOf("eams5-student", "schedule-table", "course-table", "cumtb")) {
    override fun generateCourseList() = WakeUpCompat.parseCumtb(source).ifEmpty { super.generateCourseList() }
}
class JwXjuParser(source: String) : WakeUpMarkerParser(source, setOf("xjtu", "xju", "courseTable", "课程表"))
