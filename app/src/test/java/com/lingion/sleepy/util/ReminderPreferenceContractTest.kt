package com.lingion.sleepy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReminderPreferenceContractTest {
    private data class PreferenceContract(
        val getter: String,
        val setter: String,
        val key: String,
        val default: String,
    )

    private fun readAppPrefsSource(): String = sequenceOf(
        System.getProperty("sleepy.test.root")?.let { File(it, "app/src/main/java/com/lingion/sleepy/util/AppPrefs.kt") },
        File("app/src/main/java/com/lingion/sleepy/util/AppPrefs.kt"),
        File("src/main/java/com/lingion/sleepy/util/AppPrefs.kt")
    ).firstOrNull { it?.isFile == true }?.readText() ?: error("Cannot find AppPrefs.kt")

    private fun extractFunctionBody(source: String, functionName: String): String {
        val declarationStart = source.indexOf("fun $functionName(")
        check(declarationStart >= 0) { "Missing function: $functionName" }

        // 找函数开始的 { 或 =
        val braceStart = source.indexOf('{', declarationStart)
        val equalsPos = source.indexOf('=', declarationStart)

        // 情况1: 有 { 且在 = 前面 = 块函数体
        if (braceStart >= 0 && (equalsPos < 0 || braceStart < equalsPos)) {
            var depth = 0
            for (index in braceStart until source.length) {
                when (source[index]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return source.substring(braceStart + 1, index)
                    }
                }
            }
            error("Unbalanced braces in: $functionName")
        }

        // 情况2: expression body (fun x() = expression)
        // 提取 = 后面到函数体结束的所有内容
        if (equalsPos >= 0) {
            // 找到 = 后的下一行开始
            var start = equalsPos + 1
            while (start < source.length && (source[start] == ' ' || source[start] == '\n' || source[start] == '\r' || source[start] == '\t')) {
                start++
            }
            // 找到这一行（多行expression body）的结束
            var lineEnd = start
            while (lineEnd < source.length && source[lineEnd] != '\n' && source[lineEnd] != '\r') {
                lineEnd++
            }
            // 去掉行尾空格
            while (lineEnd > start && (source[lineEnd - 1] == ' ' || source[lineEnd - 1] == '\t')) {
                lineEnd--
            }
            return source.substring(start, lineEnd)
        }

        error("Cannot extract body for: $functionName")
    }

    private fun normalized(body: String): String = body.replace(Regex("\\s+"), " ").trim()

    @Test
    fun `reminder date preferences are independent from grey holiday preferences`() {
        val source = readAppPrefsSource()
        assertTrue(source.contains("holiday_reminder_rules_enabled"))
        assertTrue(source.contains("holiday_reminder_public_holiday"))
        assertTrue(source.contains("holiday_reminder_transfer_holiday"))
        assertTrue(source.contains("holiday_reminder_makeup_workday"))
        assertTrue(source.contains("holiday_reminder_ordinary_weekend"))
        assertFalse(source.contains("isHolidayGreyHoliday(ctx)"))
    }

    @Test
    fun `each getter uses its own key and default`() {
        val source = readAppPrefsSource()
        contracts().forEach { contract ->
            assertEquals(
                "sp(ctx).getBoolean(${contract.key}, ${contract.default})",
                normalized(extractFunctionBody(source, contract.getter)),
            )
        }
    }

    @Test
    fun `each setter body contains only its matching persistent write`() {
        val source = readAppPrefsSource()
        contracts().forEach { contract ->
            assertEquals(
                "sp(ctx).edit().putBoolean(${contract.key}, v).apply()",
                normalized(extractFunctionBody(source, contract.setter)),
            )
        }
    }

    @Test
    fun `master setter cannot reset child preferences`() {
        val source = readAppPrefsSource()
        val masterBody = normalized(
            extractFunctionBody(source, "setHolidayReminderRulesEnabled"),
        )
        assertEquals(
            "sp(ctx).edit().putBoolean(KEY_HOLIDAY_REMINDER_RULES_ENABLED, v).apply()",
            masterBody,
        )
        contracts().drop(1).forEach { contract ->
            assertFalse(masterBody.contains(contract.key))
        }
    }

    private fun contracts(): List<PreferenceContract> = listOf(
        PreferenceContract(
            getter = "isHolidayReminderRulesEnabled",
            setter = "setHolidayReminderRulesEnabled",
            key = "KEY_HOLIDAY_REMINDER_RULES_ENABLED",
            default = "false",
        ),
        PreferenceContract(
            getter = "isHolidayReminderPublicHolidayEnabled",
            setter = "setHolidayReminderPublicHolidayEnabled",
            key = "KEY_HOLIDAY_REMINDER_PUBLIC_HOLIDAY",
            default = "true",
        ),
        PreferenceContract(
            getter = "isHolidayReminderTransferHolidayEnabled",
            setter = "setHolidayReminderTransferHolidayEnabled",
            key = "KEY_HOLIDAY_REMINDER_TRANSFER_HOLIDAY",
            default = "true",
        ),
        PreferenceContract(
            getter = "isHolidayReminderMakeupWorkdayEnabled",
            setter = "setHolidayReminderMakeupWorkdayEnabled",
            key = "KEY_HOLIDAY_REMINDER_MAKEUP_WORKDAY",
            default = "true",
        ),
        PreferenceContract(
            getter = "isHolidayReminderOrdinaryWeekendEnabled",
            setter = "setHolidayReminderOrdinaryWeekendEnabled",
            key = "KEY_HOLIDAY_REMINDER_ORDINARY_WEEKEND",
            default = "true",
        ),
    )
}
