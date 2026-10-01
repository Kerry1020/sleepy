package com.lingion.sleepy.ui.screen.imports

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the browser-like URL bar wiring.
 *
 * The URL shown in the bar must come from WebView navigation callbacks, and URL
 * submission must be restricted to HTTP(S) before calling loadUrl.
 */
class JwUrlBarContractTest {
    private fun loadSource(name: String): String {
        val rel = "app/src/main/java/com/lingion/sleepy/ui/screen/imports/$name"
        val userDir = System.getProperty("user.dir") ?: ""
        val fromAppDir = if (userDir.endsWith("/app")) {
            "$userDir/src/main/java/com/lingion/sleepy/ui/screen/imports/$name"
        } else null
        return sequenceOf(
            java.io.File(rel),
            fromAppDir?.let { java.io.File(it) },
            java.io.File("/private/tmp/jw-url-address-bar/$rel"),
        ).filterNotNull().firstOrNull { it.isFile }?.readText()
            ?: error("Unable to load $name")
    }

    private val builder by lazy { loadSource("JwWebViewClientBuilder.kt") }
    private val screen by lazy { loadSource("JwWebViewLoginScreen.kt") }

    @Test
    fun builder_forwards_navigation_url_without_reading_webViewUrl() {
        assertTrue(builder.contains("onUrlChanged: (String?) -> Unit = {}"))
        assertTrue(builder.contains("doUpdateVisitedHistory"))
        assertTrue(builder.contains("onUrlChanged(url)"))
    }

    @Test
    fun screen_keeps_url_draft_separate_from_live_navigation_url() {
        assertTrue(screen.contains("var currentUrl by remember"))
        assertTrue(screen.contains("var urlDraft by remember"))
        assertTrue(screen.contains("var editingUrl by remember"))
        assertTrue(screen.contains("if (!editingUrl &&"))
    }

    @Test
    fun screen_submits_only_http_or_https_urls() {
        assertTrue(screen.contains("(uri.scheme == \"http\" || uri.scheme == \"https\")"))
        assertTrue(screen.contains("loadUrl(normalized)"))
    }

    @Test
    fun screen_seeds_edit_draft_from_live_url_and_exits_edit_before_back() {
        assertTrue(screen.contains("text = currentUrl"))
        assertTrue(screen.contains("TextRange(currentUrl.length)"))
        assertTrue(screen.contains("if (editingUrl) editingUrl = false else onBack()"))
    }

    @Test
    fun screen_hides_secondary_actions_while_editing_to_preserve_url_field_width() {
        assertTrue(screen.contains("if (!editingUrl) {"))
        assertTrue(screen.contains("desktopUa = !desktopUa"))
        assertTrue(screen.contains("webViewRef?.reload()"))
    }

    @Test
    fun screen_keeps_cursor_state_in_textFieldValue_seeded_at_end() {
        // TextFieldValue (text + selection) — selection 随用户拖拽/输入保持;
        // 进编辑态时 TextRange(length) 把光标播种到末尾, 横向滚动跟随光标
        assertTrue(screen.contains("mutableStateOf(TextFieldValue("))
        assertTrue(screen.contains("TextRange(currentUrl.length)"))
        assertTrue(screen.contains("urlDraft.text.trim()"))
    }
}
