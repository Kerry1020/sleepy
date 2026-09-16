package com.lingion.sleepy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lingion.sleepy.ui.theme.SleepyTheme

/**
 * 弹窗动作按钮行 — 全 app 弹窗 confirm/dismiss 唯一入口 (2026-09-16 用户:
 * 裸 TextButton 纯文字无背景, 用户看不出是可点的色块, 也不知道按钮边界在哪)。
 *
 * 视觉 = 与全屏按钮同语言 (SleepyTheme.Buttons 高度/圆角): 确认键 primary 色块,
 * 取消/第三键 secondaryContainer 色块; 危险确认传 destructive=true 确认键变
 * errorContainer; 第三键破坏性传 thirdDestructive=true 第三键变 errorContainer
 * (教务导入退出确认: 继续/留草稿=secondary, 退出并删除草稿=error)。
 * 等宽 weight(1f) 一行排开 — 三个键也同排。
 *
 * 用法 (M3 AlertDialog 的 confirm/dismiss 槽不吃全宽布局 — 放 text 末尾):
 * ```
 * AlertDialog(
 *   ...,
 *   text = { Column { ...; Spacer(20.dp); DialogActionButtons(...) } },
 *   confirmButton = {}, dismissButton = {}
 * )
 * ```
 * 也可独立用于 ModalBottomSheet 等非 AlertDialog 容器。
 */
@Composable
fun DialogActionButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismiss: () -> Unit = {},
    thirdText: String? = null,
    onThird: () -> Unit = {},
    destructive: Boolean = false,
    thirdDestructive: Boolean = false,
    confirmEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = SleepyTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dismissText != null) {
            BlockButton(
                text = dismissText,
                onClick = onDismiss,
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        if (thirdText != null) {
            BlockButton(
                text = thirdText,
                onClick = onThird,
                container = if (thirdDestructive) colors.errorContainer else colors.secondaryContainer,
                content = if (thirdDestructive) colors.onErrorContainer else colors.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        BlockButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            container = if (destructive) colors.errorContainer else colors.primary,
            content = if (destructive) colors.onErrorContainer else colors.onPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BlockButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(SleepyTheme.Buttons.regularHeight),
        shape = SleepyTheme.Buttons.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
