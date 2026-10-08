package com.lingion.sleepy

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 编辑页 "自动跳到用户点击的时段" 接线契约。
 *
 * 行为：用户从周/网格/今日视图点某门课的某颗胶囊 → 课程详情 sheet → 编辑 →
 * 编辑页的 LazyColumn 必须 animateScrollToItem 到匹配 (day, startNode) 的卡。
 *
 * 仓库目前没有 Compose UI 测试环境，这里锁定最容易回退的接线点：
 * rememberLazyListState 必被使用、LazyColumn 必接 state、LaunchedEffect 必按
 * editingCourse.day+startNode 查 meetingBlocks 并 animateScrollToItem。
 */
class EditCourseJumpToClickedSlotTest {

    private val source: String by lazy {
        sequenceOf(
            File("app/src/main/java/com/lingion/sleepy/ui/screen/edit/AddCourseScreen.kt"),
            File("src/main/java/com/lingion/sleepy/ui/screen/edit/AddCourseScreen.kt")
        ).first { it.isFile }.readText()
    }

    @Test
    fun `LazyColumn state is bound to rememberLazyListState`() {
        assertTrue("必须 import rememberLazyListState", source.contains("import androidx.compose.foundation.lazy.rememberLazyListState"))
        assertTrue("必须有 val listState = rememberLazyListState()", source.contains("rememberLazyListState()"))
        // LazyColumn 形参 state = listState — 唯一决定滚动接管权归此 state
        assertTrue("LazyColumn 必须接 state = listState", source.contains("state = listState"))
    }

    @Test
    fun `scroll target is matched by editing course day and startNode`() {
        // 匹配表达式必须按 editingCourse 的 (day, startNode) 找 meetingBlocks 里
        // 对应的卡 — 这是"点哪颗跳哪张"语义的关键。
        assertTrue(
            "必须按 eg.day in block.days && eg.startNode == block.startNode 找目标卡",
            source.contains("eg.day in block.days") &&
                source.contains("eg.startNode == block.startNode")
        )
    }

    @Test
    fun `scroll is triggered only when target index is past the first block`() {
        // 首块就是用户点的 → 已经在视线内, 滚反而抖动。targetIdx>0 才 animateScrollToItem。
        assertTrue(
            "必须 targetIdx > 0 才 animateScrollToItem(targetIdx)",
            source.contains("if (targetIdx > 0)") &&
                source.contains("listState.animateScrollToItem(targetIdx)")
        )
    }

    @Test
    fun `scroll effect waits for group blocks to finish loading`() {
        // meetingBlocks 初始只有 1 个 initialMeetingBlock, group 加载完才到全量。
        // 用 meetingBlocks.size 当 key 让 effect 在 size 跳变后再算 targetIdx,
        // 否则拿到的是临时首块的 0 → 不滚 → 后续 group 加载完也不再触发。
        val launchEffectKeys = Regex(
            "LaunchedEffect\\([^)]*editingCourse\\?\\.id[^)]*meetingBlocks\\.size[^)]*\\)"
        )
        assertTrue(
            "必须 LaunchedEffect(editingCourse?.id, meetingBlocks.size) 等 group 加载",
            launchEffectKeys.containsMatchIn(source)
        )
    }
}
