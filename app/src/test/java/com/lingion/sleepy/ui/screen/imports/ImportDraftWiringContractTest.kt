package com.lingion.sleepy.ui.screen.imports

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导入草稿箱 UI 接线契约 — 交叉验证发现两处断链后锁死(先例:
 * ScheduleViewModeSessionContractTest — 仓库无 Robolectric/Compose UI 测试,
 * 声明式接线读源头文件等价于读编译产物):
 *
 * 断链① ManagementPage 收到 drafts/onRestoreDraft/onDeleteDraft 后调 ImportSheet
 *   时没透传, 落默认 emptyList()/no-op → 草稿图标能开面板但列表永远空,
 *   恢复/删除按钮全部无效。锁: ImportSheet( 调用位必须出现三参数实名传递。
 * 断链② SelectSchool stage 无 BackHandler → 系统返回键直接 finish Activity
 *   绕过退出确认(activeImport 置位后误触返回 = 静默丢进度, issue#39 痛点本体)。
 *   锁: JwImportActivity 必须存在 SelectSchool 条件 BackHandler。
 */
class ImportDraftWiringContractTest {

    private fun loadSource(vararg relPaths: String): String =
        sequenceOf(
            java.io.File("app/src/main/java/com/lingion/sleepy/"),
            java.io.File("src/main/java/com/lingion/sleepy/"),
        ).firstOrNull { it.isDirectory }?.let { root ->
            relPaths.map { java.io.File(root, it) }.firstOrNull { it.isFile }?.readText()
        } ?: error("Unable to load sources: ${relPaths.joinToString()}")

    /**
     * 从 [src] 提取 marker 起始的调用块: 实参表含嵌套 lambda(onJwImportRequested = { ... }),
     * 纯括号配对会被 lambda 内的 () 提前截断 — 必须 () 与 {} 双计数,
     * 括号深度归零且不在任何花括号块内才算调用结束。
     */
    private fun balancedBlock(src: String, marker: String): String {
        val start = src.indexOf(marker)
        var parenDepth = 0
        var braceDepth = 0
        for (i in start until src.length) {
            when (src[i]) {
                '(' -> parenDepth++
                ')' -> {
                    parenDepth--
                    // 实参级 '(' 已闭 + 无未闭 lambda → 本次调用结束
                    if (parenDepth == 0 && braceDepth == 0) return src.substring(start, i + 1)
                }
                '{' -> braceDepth++
                '}' -> braceDepth--
            }
        }
        return src.substring(start)
    }

    @Test
    fun `ManagementPage forwards draft params into ImportSheet call site`() {
        val src = loadSource("ui/screen/manage/ManagementPage.kt")
        val callSite = balancedBlock(src, "ImportSheet(")
        // 实名透传三件套必须出现在 ImportSheet( 的实参表里
        assertTrue(
            "ImportSheet( 调用位缺少 drafts = drafts 实名透传",
            Regex("""drafts\s*=\s*drafts""").containsMatchIn(callSite),
        )
        assertTrue(
            "ImportSheet( 调用位缺少 onRestoreDraft 透传",
            Regex("""onRestoreDraft\s*=\s*onRestoreDraft""").containsMatchIn(callSite),
        )
        assertTrue(
            "ImportSheet( 调用位缺少 onDeleteDraft 透传",
            Regex("""onDeleteDraft\s*=\s*onDeleteDraft""").containsMatchIn(callSite),
        )
    }

    @Test
    fun `SelectSchool stage has conditional BackHandler for exit confirmation`() {
        val src = loadSource("ui/screen/imports/JwImportActivity.kt")
        assertTrue(
            "SelectSchool stage 缺 BackHandler — 系统返回键会绕过退出确认直接 finish",
            Regex(
                """BackHandler\s*\(\s*enabled\s*=.*Stage\.SelectSchool"""
            ).containsMatchIn(src),
        )
        // BackHandler 回调必须走 requestExit() (弹三选确认), 禁直连 finish()
        val handler = src.substringAfter("BackHandler(enabled").substringBefore("LaunchedEffect(incomingDraftId)")
        assertTrue(
            "SelectSchool BackHandler 必须经 requestExit() 弹确认, 禁直连 finish()",
            handler.contains("requestExit()"),
        )
    }

    @Test
    fun `ImportSheet declares draft params with forwarding-ready names`() {
        val src = loadSource("ui/screen/imports/ImportSheet.kt")
        assertTrue(
            "ImportSheet 签名缺 drafts 形参",
            Regex("""drafts:\s*List<ImportDraft>""").containsMatchIn(src),
        )
        assertTrue(
            "ImportSheet 签名缺 onRestoreDraft 形参",
            Regex("""onRestoreDraft:\s*\(String\)\s*->\s*Unit""").containsMatchIn(src),
        )
        assertTrue(
            "ImportSheet 必须把 onRestoreDraft 接到 ImportDraftSheet 的 onRestore",
            Regex("""onRestore\s*=\s*\{[^}]*onRestoreDraft""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(src),
        )
    }
}
