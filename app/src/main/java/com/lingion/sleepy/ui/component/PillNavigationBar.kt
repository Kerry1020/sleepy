package com.lingion.sleepy.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.InnerShadow
import com.lingion.sleepy.ui.theme.SleepyTheme
import com.lingion.sleepy.ui.theme.noRippleClickable
import kotlinx.coroutines.launch

/**
 * 底栏单项数据。v2 起组件内部要管理 thumb 滑块几何(目标位置/逐字符扫过变色),
 * content lambda 拿不到子项坐标, API 从 { PillNavItem(...) } 改为数据驱动。
 */
data class PillNavItemSpec(val icon: ImageVector, val label: String, val badge: Boolean = false)

/**
 * 底栏 thumb 高亮状态 — 由调用方(AppRoot)在 NavDisplay 之外持有。
 *
 * 2026-09-21 用户报障: 底栏在 navigation3 里属于 entry<Main>,push 子页时整个底栏
 * 被销毁,pop 返回是全新组合 — Animatable 归零后首帧按「thumb 在 0 位」给课表 tab
 * 上色,几何量测完才 snap 回正确 tab,肉眼可见高亮「从首页挪到目标 tab」。
 * 状态提升到 entries 之外后,pop 重建时 thumb 位置/覆盖色从第一帧就正确。
 */
class PillBarState {
    val thumbX = Animatable(0f)
    var thumbPlaced by mutableStateOf(false)
    var thumbCenter by mutableFloatStateOf(0f)
    // 上次落位时的 tab 序号 — 重建组合与持久落位一致时禁止任何动画/重定位,
    // 高亮从首帧起钉死在原位(2026-09-21 用户: 从子页返回时光点不该动)。
    var placedIndex by mutableIntStateOf(-1)
}

/**
 * Dock(悬浮)模式下主内容需要的额外底部滚动余量 — 让最后一项能滚到 Dock 上方完全可见
 * (FAB 语义: 内容画到屏幕底, 但滚动范围多留遮挡物的高度)。贴底模式为 0dp。
 * MainActivity 在 dock 分支 provide Dock 总高, 四个 tab 页的滚动容器读取追加。
 */
val LocalNavExtraBottomPadding = compositionLocalOf { 0.dp }

/** Dock 形态常量 — 对齐 iOS 26 Liquid Glass TabBar 真实测量值 */
object NavDockSpec {
    val horizontalMargin = 21.dp   // iOS 26: 21pt from screen edge (learnui.design)
    val bottomFloat = 21.dp        // iOS 26: 21pt from home indicator (learnui.design)
    val capsuleHeight = 56.dp      // iOS 26: ~54-56pt capsule (community measurement)
    val itemSeat = 86.dp           // iOS 26: 86pt center-to-center (adaptive_liquid_glass)
    val itemGap = 0.dp             // 86dp 已含间距, 不再额外加
    val innerPad = 14.dp           // iOS 26: ~14pt internal padding
    val iconSize = 25.dp           // iOS 26: 25pt (uiuxdesigning.com)
    val thumbWidth = 78.dp         // Kyant demo: pressedScale = 78/56 ≈ 1.393
    val thumbHeight = 48.dp        // 适配 56dp capsule, 上下各留 4dp
    val thumbYOffset = 4.dp        // (56 - 48) / 2 = 4dp 垂直居中
}

/**
 * 底部导航栏 (v2)
 *
 * 动画: 与 SegmentedSwitcher 同一套 — 单个 secondaryContainer thumb 色块由物理弹簧
 * (Animatable+spring) 在药丸带上从旧 tab 滑到新 tab; 图标按「图标区间 ∩ thumb 区间」
 * 覆盖率 lerp 变色, 文字逐字符按「字符矩形 ∩ thumb 区间」扫过上色 — 色块边缘扫过哪个字
 * 那个字才变色。thumb 位置 graphicsLayer 直读弹簧浮点值(无 placeRelative 取整量化),
 * 高刷屏下是连续非线性曲线。图标/文字逐帧重组着色(4 项小成本, 同 SegmentedSwitcher 先例)。
 *
 * 几何真值: 各 tab 药丸中心/文字左缘/bar 原点全部 onGloballyPositioned 实测(positionInRoot
 * 差值), SpaceEvenly + 变宽 label 的实际分布不靠推算。布局变化(旋转/语言/形态)时
 * centers key 变化 → LaunchedEffect 重跑 → thumb 平滑滑到新位置。
 */
@Composable
fun PillNavigationBar(
    items: List<PillNavItemSpec>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dock: Boolean = false,
    state: PillBarState = remember { PillBarState() },
    backdrop: Backdrop? = null,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val count = items.size.coerceAtLeast(1)
    val pillHalf = with(density) { 32.dp.toPx() }
    val iconHalf = with(density) { 10.dp.toPx() }

    // ── 几何真值(root 坐标, bar 相对坐标在读取时现算) ──
    var barRootX by remember { mutableFloatStateOf(0f) }
    var barRootY by remember { mutableFloatStateOf(0f) }
    var barLaidOut by remember { mutableStateOf(false) }
    val pillRootXs = remember(count) { mutableStateListOf<Float>().apply { repeat(count) { add(0f) } } }
    var pillRootY by remember { mutableFloatStateOf(0f) }
    val labelRootXs = remember(count) { mutableStateListOf<Float>().apply { repeat(count) { add(0f) } } }
    // 各 tab 文字字符矩形(文本局部坐标, onTextLayout 时缓存 — 非状态, 着色帧顺带读)
    val charRectsCache = remember { mutableMapOf<Int, List<Rect>>() }

    // 读 pillRootXs/barRoot → 布局变化时重启 effect 重新定目标。
    // Main entry 从子页返回时会重新组合: 此次组合的首次测量只 snap,不能把同一 tab
    // 的几何重测误判成 tab 切换,否则高亮会从旁边滑回原位。
    var laidOutInThisComposition by remember { mutableStateOf(false) }
    val centers = List(count) { pillRootXs[it] + pillHalf - barRootX }
    LaunchedEffect(centers, barLaidOut, selectedIndex) {
        val idx = selectedIndex.coerceIn(0, count - 1)
        // 组合重建早期的 root 坐标仍是 0/临时值,不能据此重定位持久 thumb。
        if (!barLaidOut || pillRootXs[idx] <= 0f) return@LaunchedEffect
        val target = centers[idx]
        if (state.placedIndex == idx && state.thumbPlaced) {
            laidOutInThisComposition = true
            return@LaunchedEffect
        }
        if (!laidOutInThisComposition) {
            state.thumbX.snapTo(target)
            state.thumbCenter = target
            state.thumbPlaced = true
            state.placedIndex = idx
            laidOutInThisComposition = true
        } else {
            // 只有 selectedIndex 真正变化才走弹簧;同 tab 的重测量永远不动。
            state.thumbX.animateTo(
                targetValue = target,
                animationSpec = SleepyThumbSpring,
                block = { state.thumbCenter = value }
            )
            state.placedIndex = idx
        }
    }

    // ═══ 形态分流 ═══
    // 贴底 = 通栏 76dp 矩形带(现状不动); Dock = iOS 26 floating tab bar 语义:
    // 56dp 胶囊 icon-only, 半透明玻璃底+柔投影, 悬于手势条上方, 浮在内容上层
    if (dock) {
        DockNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            colors = colors,
            density = density,
            onBarGeometry = { x, y, laid ->
                barRootX = x; barRootY = y; barLaidOut = laid
            },
            modifier = modifier,
            state = state,
            backdrop = backdrop,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 6.dp, bottom = 8.dp)
            .onGloballyPositioned { c ->
                barRootX = c.positionInRoot().x
                barRootY = c.positionInRoot().y
                barLaidOut = true
            }
    ) {
        // 层0: thumb 色块 — graphicsLayer 直读弹簧浮点值, 无整数取整, 高刷连续
        // 首次定位前 alpha 0(否则 thumb 停在 x=0 闪现左上角)
        Box(
            Modifier
                .size(width = 64.dp, height = 32.dp)
                .graphicsLayer {
                    alpha = if (state.thumbPlaced) 1f else 0f
                    translationX = state.thumbX.value - pillHalf
                    translationY = pillRootY - barRootY
                }
                .clip(SleepyTheme.shapes.large)
                .background(colors.secondaryContainer)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, item ->
                val isSel = i == selectedIndex
                val tc = state.thumbCenter
                val thumbStart = tc - pillHalf
                val thumbEnd = tc + pillHalf

                // 图标: 图标区间 ∩ thumb 区间 覆盖率
                val iconCov = intervalCoverage(
                    thumbStart, thumbEnd,
                    pillRootXs[i] - barRootX - iconHalf,
                    pillRootXs[i] - barRootX + iconHalf
                )
                // 文字: 逐字符扫过(NaN = 字形未就绪 → 退化整词翻转)
                val rects = charRectsCache[i]
                val labelCov = rects?.let {
                    sweepCoverageAt(it, thumbStart, thumbEnd, labelRootXs[i] - barRootX)
                } ?: Float.NaN
                val t = if (labelCov.isNaN()) (if (isSel) 1f else 0f) else labelCov
                val labelColor = lerp(colors.onSurfaceVariant, colors.onSurface, t)

                Column(
                    modifier = Modifier
                        .noRippleClickable { onSelect(i) }
                        .semantics { role = Role.Tab; selected = isSel }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 32.dp)
                            .onGloballyPositioned { c ->
                                pillRootXs[i] = c.positionInRoot().x
                                pillRootY = c.positionInRoot().y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = lerp(colors.onSurfaceVariant, colors.onSurface, iconCov),
                                modifier = Modifier.size(20.dp)
                            )
                            if (item.badge) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .size(7.dp)
                                        .background(colors.primary, CircleShape)
                                )
                            }
                        }
                    }
                    Text(
                        text = item.label,
                        onTextLayout = { result ->
                            // 字形只在文本/字重变化时重排, 缓存避免每帧重算字符矩形
                            charRectsCache[i] = result.charRects()
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = labelColor,
                        modifier = Modifier.onGloballyPositioned { c ->
                            labelRootXs[i] = c.positionInRoot().x
                        }
                    )
                }
            }
        }
    }
}

/** 区间 [aStart,aEnd] 对 [bStart,bEnd] 的横向覆盖率 0..1 */
private fun intervalCoverage(aStart: Float, aEnd: Float, bStart: Float, bEnd: Float): Float {
    val w = bEnd - bStart
    if (w <= 0f) return 0f
    return (aEnd.coerceAtMost(bEnd) - aStart.coerceAtLeast(bStart)).coerceIn(0f, w) / w
}

/**
 * Dock 悬浮胶囊 — 全量对齐 iOS 26 Liquid Glass TabBar (2026-10-29 v4)
 *
 * 三层结构(参照 Kyant Backdrop demo LiquidBottomTabs):
 *   Layer 1: 外层容器(56dp) — vibrancy + blur 8dp + lens 24/24 + tint
 *   Layer 2: Tabs backdrop(accent tint backdrop 源, 用 opacity effect 隐藏)
 *   Layer 3: 活动 tab 胶囊(滑动) — lens 10/14 + chromaticAberration +
 *            highlight + shadow + innerShadow + surfaceTint, 全部 * pressProgress
 *
 * Press 动效: touchDown → pressProgress=1 (spring 0.5/300),
 *             touchUp → pressProgress=0 (spring 1.0/1000)
 *             驱动 Layer 3 效果强度 + tab scale lerp(1.0, 1.2) + 容器微缩放
 *
 * 几何对齐 iOS 26: 21dp 边距, 86dp tab 间距, 56dp capsule, 25dp icon。
 * 窄屏动态回落: availableWidth < fullWidth 时自动等分座位。
 * RTL: centers 按 LayoutDirection 镜像。
 *
 * API 分档: 33+ 全量 / 31-32 blur 半玻璃 / ≤30 平面胶囊
 */
@Composable
private fun DockNavigationBar(
    items: List<PillNavItemSpec>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    colors: androidx.compose.material3.ColorScheme,
    density: androidx.compose.ui.unit.Density,
    onBarGeometry: (Float, Float, Boolean) -> Unit,
    state: PillBarState,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val count = items.size.coerceAtLeast(1)
    val thumbHalf = with(density) { (NavDockSpec.thumbWidth / 2).toPx() }
    val iconHalf = with(density) { (NavDockSpec.iconSize / 2).toPx() }

    val capsuleShape = RoundedCornerShape(percent = 50)
    val glassBg = colors.surfaceContainer.copy(alpha = 0.86f)

    // ── 玻璃分档 ──
    val bd = backdrop
    val sdk = android.os.Build.VERSION.SDK_INT
    val isLight = !androidx.compose.foundation.isSystemInDarkTheme()
    val glassTint = if (isLight) {
        androidx.compose.ui.graphics.Color(0xFFFAFAFA).copy(alpha = 0.4f)
    } else {
        androidx.compose.ui.graphics.Color(0xFF121212).copy(alpha = 0.4f)
    }
    val accentColor = if (isLight) {
        androidx.compose.ui.graphics.Color(0xFF0088FF)
    } else {
        androidx.compose.ui.graphics.Color(0xFF0091FF)
    }

    // ── Press 动效 ──
    val pressProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // ── 动态紧凑布局: 可用宽度不够时自动缩小座位 ──
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val layoutDir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val availableWidthDp = config.screenWidthDp.dp - NavDockSpec.horizontalMargin * 2
    val fullWidth = NavDockSpec.itemSeat * count + NavDockSpec.innerPad * 2
    val useCompactLayout = availableWidthDp < fullWidth
    val effectiveSeat = if (useCompactLayout) {
        (availableWidthDp - NavDockSpec.innerPad * 2) / count
    } else {
        NavDockSpec.itemSeat
    }
    val effectiveInnerPad = if (useCompactLayout) 8.dp else NavDockSpec.innerPad
    val totalWidth = effectiveSeat * count + effectiveInnerPad * 2

    // ── 座位中心(RTL 感知) ──
    val isRtl = layoutDir == androidx.compose.ui.unit.LayoutDirection.Rtl
    val seatHalfPx = with(density) { (effectiveSeat / 2).toPx() }
    val centers = List(count) { i ->
        with(density) {
            val ltrPos = effectiveInnerPad.toPx() + seatHalfPx + effectiveSeat.toPx() * i
            if (isRtl) totalWidth.toPx() - ltrPos else ltrPos
        }
    }

    var laidOutInThisComposition by remember { mutableStateOf(false) }
    LaunchedEffect(centers, selectedIndex) {
        val idx = selectedIndex.coerceIn(0, count - 1)
        val target = centers[idx]
        if (state.placedIndex == idx && state.thumbPlaced) {
            laidOutInThisComposition = true
            return@LaunchedEffect
        }
        if (!laidOutInThisComposition) {
            state.thumbX.snapTo(target)
            state.thumbCenter = target
            state.thumbPlaced = true
            state.placedIndex = idx
            laidOutInThisComposition = true
        } else {
            state.thumbX.animateTo(
                targetValue = target,
                animationSpec = SleepyThumbSpring,
                block = { state.thumbCenter = value }
            )
            state.placedIndex = idx
        }
    }

    // ── Layer 2 backdrop (only needed when Layer 3 will consume it, i.e. API 33+) ──
    val tabsBackdrop = if (bd != null && sdk >= 33) rememberLayerBackdrop() else null

    // ── Surface modifier: Layer 1 container ──
    val surfaceModifier = if (bd != null && sdk >= 31) {
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = NavDockSpec.horizontalMargin)
            .padding(bottom = NavDockSpec.bottomFloat)
            .width(totalWidth)
            .height(NavDockSpec.capsuleHeight)
            .graphicsLayer {
                // 容器 press 微缩放(Kyant demo: lerp(1.0, 1+16dp/w, progress))
                val containerScale = 1f + (16f / totalWidth.toPx()) * pressProgress.value
                scaleX = containerScale
                scaleY = containerScale
            }
            .drawBackdrop(
                backdrop = bd,
                shape = { capsuleShape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    if (sdk >= 33) lens(24.dp.toPx(), 24.dp.toPx(), chromaticAberration = false)
                },
                onDrawSurface = { drawRect(glassTint) },
            )
    } else {
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = NavDockSpec.horizontalMargin)
            .padding(bottom = NavDockSpec.bottomFloat)
            .shadow(elevation = 12.dp, shape = capsuleShape, clip = false,
                ambientColor = androidx.compose.ui.graphics.Color.Black,
                spotColor = androidx.compose.ui.graphics.Color.Black)
            .width(totalWidth)
            .height(NavDockSpec.capsuleHeight)
            .clip(capsuleShape)
            .background(glassBg)
    }

    Box(
        modifier = surfaceModifier
            .onGloballyPositioned { c ->
                onBarGeometry(c.positionInRoot().x, c.positionInRoot().y, true)
            }
    ) {
        // ── Layer 2: accent tint backdrop source ──
        // 用 opacity(0) effect 代替 graphicsLayer alpha=0, 确保 layerBackdrop 仍能捕获
        if (tabsBackdrop != null && bd != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NavDockSpec.capsuleHeight)
                    .layerBackdrop(tabsBackdrop)
                    .drawBackdrop(
                        backdrop = bd,
                        shape = { capsuleShape },
                        effects = {
                            opacity(0f) // 隐藏视觉输出但保留 backdrop 捕获
                            vibrancy()
                            blur(8.dp.toPx())
                        },
                        onDrawSurface = { drawRect(accentColor) },
                    ),
                horizontalArrangement = Arrangement.spacedBy(NavDockSpec.itemGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { _, _ ->
                    Box(Modifier.size(effectiveSeat, NavDockSpec.capsuleHeight))
                }
            }
        }

        // ── Layer 3: Active tab capsule ──
        if (bd != null && sdk >= 33 && tabsBackdrop != null) {
            val combinedBd = rememberCombinedBackdrop(bd, tabsBackdrop)
            Box(
                Modifier
                    .offset(y = NavDockSpec.thumbYOffset)
                    .size(width = NavDockSpec.thumbWidth, height = NavDockSpec.thumbHeight)
                    .graphicsLayer {
                        val pp = pressProgress.value
                        alpha = if (state.thumbPlaced) 1f else 0f
                        translationX = state.thumbX.value - thumbHalf
                        val scale = 1f + (78f / 56f - 1f) * pp
                        scaleX = scale
                        scaleY = scale
                    }
                    .drawBackdrop(
                        backdrop = combinedBd,
                        shape = { RoundedCornerShape(percent = 50) },
                        effects = {
                            val pp = pressProgress.value
                            lens(
                                10.dp.toPx() * pp,
                                14.dp.toPx() * pp,
                                chromaticAberration = pp > 0.05f
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = pressProgress.value) },
                        shadow = { Shadow.Default.copy(alpha = pressProgress.value) },
                        innerShadow = {
                            val pp = pressProgress.value
                            InnerShadow(
                                radius = 8.dp * pp,
                                offset = androidx.compose.ui.unit.DpOffset(0.dp, 2.dp * pp),
                                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.15f),
                                alpha = pp
                            )
                        },
                        onDrawSurface = {
                            val pp = pressProgress.value
                            drawRect(
                                if (isLight) androidx.compose.ui.graphics.Color.Black.copy(0.1f)
                                else androidx.compose.ui.graphics.Color.White.copy(0.1f),
                                alpha = 1f - pp
                            )
                            drawRect(androidx.compose.ui.graphics.Color.Black.copy(0.03f * pp))
                        },
                    )
            )
        } else {
            // Fallback thumb: secondaryContainer color block
            Box(
                Modifier
                    .offset(y = NavDockSpec.thumbYOffset)
                    .size(width = NavDockSpec.thumbWidth, height = NavDockSpec.thumbHeight)
                    .graphicsLayer {
                        alpha = if (state.thumbPlaced) 1f else 0f
                        translationX = state.thumbX.value - thumbHalf
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.secondaryContainer)
            )
        }

        // ── Tab content (icons + labels) ──
        Row(
            modifier = Modifier.fillMaxWidth().height(NavDockSpec.capsuleHeight),
            horizontalArrangement = Arrangement.spacedBy(NavDockSpec.itemGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, item ->
                val isSel = i == selectedIndex
                val tc = state.thumbCenter
                val iconCov = intervalCoverage(
                    tc - thumbHalf, tc + thumbHalf,
                    centers[i] - iconHalf, centers[i] + iconHalf
                )
                val tabScale = 1f + 0.2f * pressProgress.value * if (isSel) 1f else 0f

                Column(
                    modifier = Modifier
                        .size(effectiveSeat, NavDockSpec.capsuleHeight)
                        .graphicsLayer {
                            scaleX = tabScale
                            scaleY = tabScale
                        }
                        .pointerInput(i) {
                            detectTapGestures(
                                onPress = {
                                    scope.launch {
                                        pressProgress.animateTo(
                                            1f,
                                            spring(dampingRatio = 0.5f, stiffness = 300f)
                                        )
                                    }
                                    tryAwaitRelease()
                                    scope.launch {
                                        pressProgress.animateTo(
                                            0f,
                                            spring(dampingRatio = 1.0f, stiffness = 1000f)
                                        )
                                    }
                                },
                                onTap = { onSelect(i) }
                            )
                        }
                        .semantics {
                            role = Role.Tab
                            selected = isSel
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = lerp(colors.onSurfaceVariant, colors.onSecondaryContainer, iconCov),
                            modifier = Modifier.size(NavDockSpec.iconSize)
                        )
                        if (item.badge) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .background(colors.primary, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = lerp(colors.onSurfaceVariant, colors.onSecondaryContainer, iconCov)
                    )
                }
            }
        }
    }
}

/**
 * 逐字符扫过覆盖率: rects(文本局部坐标) + 文字左缘偏移 → 与 thumb 区间求交集权重平均。
 * 与 SegmentedSwitcher.segmentLabelColor 同一算法(横向交集/字符宽加权/lerp),
 * 这里以「文字左缘 + thumb 中心±半宽」参数化 — thumb 为中心坐标制。
 */
private fun sweepCoverageAt(
    rects: List<Rect>,
    thumbStart: Float,
    thumbEnd: Float,
    labelLeftEdge: Float
): Float {
    if (rects.isEmpty()) return Float.NaN
    val left = rects.minOf { it.left }
    val right = rects.maxOf { it.right }
    if (right - left <= 0f) return Float.NaN
    var sum = 0f
    var total = 0f
    for (r in rects) {
        val w = r.width
        if (w <= 0f) continue
        val l = r.left + labelLeftEdge
        val e = r.right + labelLeftEdge
        sum += (thumbEnd.coerceAtMost(e) - thumbStart.coerceAtLeast(l)).coerceIn(0f, w)
        total += w
    }
    if (total <= 0f) return Float.NaN
    return (sum / total).coerceIn(0f, 1f)
}

/** 字符级 bounding box 列表(无效矩形剔除; 与 SegmentedSwitcher 同一兜底) */
private fun TextLayoutResult.charRects(): List<Rect> =
    (0 until layoutInput.text.length).mapNotNull { i ->
        try {
            val r = getBoundingBox(i)
            if (r.width > 0f && r.height > 0f) r else null
        } catch (_: Exception) {
            null
        }
    }
