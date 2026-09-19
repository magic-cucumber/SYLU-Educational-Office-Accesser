package top.kagg886.eoa.component.reveal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.*
import com.svenjacobs.reveal.*
import com.svenjacobs.reveal.shapes.balloon.Arrow
import com.svenjacobs.reveal.shapes.balloon.Balloon
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/13 20:39
 * ================================================
 */

private val LocalRevealState = staticCompositionLocalOf<RevealState> {
    error("CompositionLocal LocalRevealState not provided")
}

private val LocalRevealKeyRegistry =
    staticCompositionLocalOf<MutableMap<Int, RevealOverlayRegistration>> {
        error("CompositionLocal LocalRevealKeyRegistry not provided")
    }

private typealias RevealOverlayContentFunction = @Composable (anchorBounds: IntRect) -> Unit

private data class RevealOverlayRegistration(
    val content: RevealOverlayContentFunction? = null,
    val anchorBounds: IntRect? = null,
)

enum class ContainerArrow {
    Top,
    Bottom,
    Start,
    End
}


fun Modifier.revealableAutoMeasured(
    step: Int,
    arrow: ContainerArrow,
    content: @Composable BoxScope.() -> Unit
) =
    composed {
        val registry = LocalRevealKeyRegistry.current
        val state = LocalRevealState.current

        DisposableEffect(step, content, registry, arrow) {
            require(step >= 0) {
                "step $step is not less than 0"
            }

            require(step < registry.size) {
                "step $step is not less than ${registry.size}"
            }

            require(registry[step]?.content == null) {
                "step $step is already registered revealable"
            }

            val overlayContent: RevealOverlayContentFunction = { anchorBounds ->
                MeasuredRevealBalloon(
                    anchorBounds = anchorBounds,
                    arrow = arrow,
                    content = {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            content()
                        }
                    },
                )
            }

            registry[step] = registry.getValue(step).copy(content = overlayContent)
            onDispose {
                //此时registry[step].content不可控，如果仍为原对象就移除。
                registry[step] = with(registry.getValue(step)) {
                    if (this.content != overlayContent) return@onDispose
                    this.copy(content = null)
                }
            }
        }

        DisposableEffect(step, state) {
            onDispose { state.removeRevealable(step) }
        }

        Modifier.onGloballyPositioned { coordinates ->
            // boundsInRoot 包含滚动容器等祖先的裁剪，再限制到根视口内。
            val clipped = coordinates.boundsInRoot()
            val rootSize = coordinates.findRootCoordinates().size
            val bounds = Rect(
                left = clipped.left.coerceIn(0f, rootSize.width.toFloat()),
                top = clipped.top.coerceIn(0f, rootSize.height.toFloat()),
                right = clipped.right.coerceIn(0f, rootSize.width.toFloat()),
                bottom = clipped.bottom.coerceIn(0f, rootSize.height.toFloat()),
            )
            // 气泡位于 Popup 中，使用窗口坐标；高亮注册仍使用根坐标。
            val windowBounds =
                bounds.translate(coordinates.findRootCoordinates().positionInWindow())
            registry[step] = registry.getValue(step).copy(
                anchorBounds = if (bounds.isEmpty) null else IntRect(
                    left = windowBounds.left.roundToInt(),
                    top = windowBounds.top.roundToInt(),
                    right = windowBounds.right.roundToInt(),
                    bottom = windowBounds.bottom.roundToInt(),
                ),
            )
            // 库的 revealable 使用完整 size，改为直接注册可见范围。
            // 不使用默认的 8.dp 外扩，避免高亮再次越过裁剪边界。
            state.addRevealable(
                Revealable(
                    key = step,
                    shape = RevealShape.RoundRect(8.dp),
                    padding = PaddingValues(0.dp),
                    borderStroke = null,
                    layout = Revealable.Layout(bounds.topLeft, bounds.size),
                    onClick = null,
                ),
            )
        }
    }

@Composable
private fun ContainerArrow.toBalloonArrow(
    horizontalAlignment: Alignment.Horizontal,
    verticalAlignment: Alignment.Vertical,
): Arrow = when (this) {
    ContainerArrow.Top -> Arrow.bottom(horizontalAlignment = horizontalAlignment)
    ContainerArrow.Bottom -> Arrow.top(horizontalAlignment = horizontalAlignment)
    ContainerArrow.Start -> Arrow.end(verticalAlignment = verticalAlignment)
    ContainerArrow.End -> Arrow.start(verticalAlignment = verticalAlignment)
}

@Composable
private fun MeasuredRevealBalloon(
    anchorBounds: IntRect,
    arrow: ContainerArrow,
    content: @Composable BoxScope.() -> Unit,
) {
    val horizontalArrowAlignment = remember { MeasuredHorizontalAlignment() }
    val verticalArrowAlignment = remember { MeasuredVerticalAlignment() }
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer

    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val margin = 8.dp.roundToPx()
        // 屏幕内先留出 margin，得到气泡允许使用的 viewport（窗口坐标）：
        //
        // 0              margin                              screenWidth - margin       screenWidth
        // |----------------|==========================================|----------------------|
        // |<-- margin ---->|<----------- viewport.width ------------->|<------ margin ------>|
        //
        // 正常情况下：left = margin，right = screenWidth - margin。
        // 纵向同理：top = margin，bottom = screenHeight - margin。
        // 当屏幕尺寸小于 2 * margin 时，两条边收拢到屏幕中心，避免产生负宽高。
        val viewport = IntRect(
            left = minOf(margin, constraints.maxWidth / 2),
            top = minOf(margin, constraints.maxHeight / 2),
            right = maxOf(constraints.maxWidth - margin, constraints.maxWidth / 2),
            bottom = maxOf(constraints.maxHeight - margin, constraints.maxHeight / 2),
        )
        // 96.dp / 48.dp 是允许展示带箭头提示的最小可用宽高，不是气泡的固定尺寸。
        // 首选方向放不下时换到其他方向；四侧均不满足时返回 null，在 viewport 内显示无箭头提示。
        val placement = chooseBalloonPlacement(
            anchorBounds, viewport, arrow, layoutDirection, margin,
            minWidth = 96.dp.roundToPx(), minHeight = 48.dp.roundToPx(),
        )
        val area = placement?.area ?: viewport
        // 测量约束取选中方向的 area，而不是整屏 viewport。以气泡位于目标右侧为例：
        //
        // viewport.left       anchor.right        area.left                         viewport.right
        // |------ anchor ----------|----- gap --------|====================================|
        //                                            |<---------- area.width ------------>|
        //
        // maxChildWidth = area.right - area.left，maxChildHeight = area.bottom - area.top。
        // Balloon 的箭头和内容内边距均包含在测量尺寸里，外层不再叠加 padding。
        // 长文本先按 area.width 换行，避免按整屏测量后再平移，导致气泡压住目标。
        val placeable = subcompose(placement?.arrow) {
            if (placement == null) {
                // 目标占满视口时没有可指向的外侧空间，退化为不带箭头的提示。
                Surface(color = backgroundColor, shape = MaterialTheme.shapes.small) {
                    Box(content = content)
                }
            } else {
                Balloon(
                    arrow = placement.arrow.toBalloonArrow(
                        horizontalArrowAlignment, verticalArrowAlignment,
                    ),
                    backgroundColor = backgroundColor,
                    elevation = 2.dp,
                    content = content,
                )
            }
        }.single().measure(Constraints(maxWidth = area.width, maxHeight = area.height))
        val physicalArrow = placement?.arrow?.physical(layoutDirection)
        // 沿箭头指向的轴贴近目标，沿另一轴对齐目标中心，再限制到 area 内。
        // Start/End 已转为物理 left/right，以下示意图均按物理坐标理解：
        //
        // Start（气泡在左侧）：
        // area.left           x                  area.right       anchor.left
        // |-------------------|-----------------------|---- gap -------|
        //                     |<-- placeable.width -->|<-- 指向目标 -->|
        // x = area.right - placeable.width
        //
        // End（气泡在右侧）：
        // anchor.right        area.left == x                          area.right
        // |------- gap -----------|-----------------------|-----------------|
        //                         |<-- placeable.width -->|
        // x = area.left
        //
        // Top/Bottom 的横向居中：preferredX = anchorBounds.centerX - placeable.width / 2。
        // x 的范围为 [area.left, area.right - placeable.width]，仅在目标中心靠近屏幕边缘时偏移。
        // 纵向同理：Top 贴 area.bottom，Bottom 贴 area.top；Start/End 对齐 anchorBounds.centerY。
        // 无箭头回退时，横纵两轴均尝试对齐目标中心，并限制在 viewport 内。
        val x = when (physicalArrow) {
            ContainerArrow.Start -> area.right - placeable.width
            ContainerArrow.End -> area.left
            else -> (anchorBounds.centerX - placeable.width / 2)
                .coerceWithin(area.left, area.right - placeable.width)
        }
        val y = when (physicalArrow) {
            ContainerArrow.Top -> area.bottom - placeable.height
            ContainerArrow.Bottom -> area.top
            else -> (anchorBounds.centerY - placeable.height / 2)
                .coerceWithin(area.top, area.bottom - placeable.height)
        }
        // anchorBounds 使用窗口坐标，Arrow 的 Alignment 使用 Balloon 内的局部坐标。
        // Balloon 已无外层 padding，因此只需减去气泡位置，不再减 screenMargin：
        //
        // 窗口横坐标：
        // 0                   x                    anchorBounds.centerX          x + placeable.width
        // |-------------------|-----------------------------|------------------------------|
        //                     |<--------- center ---------->|
        //                     |<--------------- placeable.width -------------------------->|
        //
        // horizontalArrowAlignment.center = anchorBounds.centerX - x
        // verticalArrowAlignment.center = anchorBounds.centerY - y（纵向同理）。
        // 这只是期望箭头中心；arrowOffset 还会限制它，避免箭头底边压到圆角。
        // cornerInset = Balloon 默认圆角 8.dp + 留白 4.dp；若修改圆角，应同步修改此值。
        horizontalArrowAlignment.center = anchorBounds.centerX - x
        verticalArrowAlignment.center = anchorBounds.centerY - y
        horizontalArrowAlignment.cornerInset = 12.dp.roundToPx()
        verticalArrowAlignment.cornerInset = 12.dp.roundToPx()
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, y) }
    }
}

private class MeasuredHorizontalAlignment : Alignment.Horizontal {
    var center: Int = 0
    var cornerInset: Int = 0

    override fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int =
        arrowOffset(center, size, space, cornerInset)
}

private class MeasuredVerticalAlignment : Alignment.Vertical {
    var center: Int = 0
    var cornerInset: Int = 0

    override fun align(size: Int, space: Int): Int =
        arrowOffset(center, size, space, cornerInset)
}

internal fun arrowOffset(center: Int, size: Int, space: Int, cornerInset: Int): Int {
    // Alignment 返回箭头底边的起点，而 center 表示期望尖端（底边中心）的位置：
    //
    // 0            inset          offset          offset + size         space - inset       space
    // |--------------|---------------|==================|---------------------|---------------|
    //                                |<----- size ----->|
    //                                         center
    //
    // preferredOffset = center - size / 2
    // 起点下界 = inset，起点上界 = space - inset - size = available - inset。
    // 当空间不足以容纳两侧 cornerInset 时，将 inset 缩为 available / 2，保证上下界有序。
    // space < size 时 available = 0，起点回退到 0，避免 coerceIn 收到无效区间。
    val available = (space - size).coerceAtLeast(0)
    val inset = minOf(cornerInset, available / 2)
    return (center - size / 2).coerceIn(inset, available - inset)
}

internal data class BalloonPlacement(val arrow: ContainerArrow, val area: IntRect)

// Start/End 是逻辑方向：RTL 下左右互换；Top/Bottom 不变。
// 区域计算和 place(x, y) 都使用物理坐标，箭头构造仍使用原逻辑方向，避免重复镜像。
private fun ContainerArrow.physical(layoutDirection: LayoutDirection): ContainerArrow = when {
    layoutDirection == LayoutDirection.Ltr -> this
    this == ContainerArrow.Start -> ContainerArrow.End
    this == ContainerArrow.End -> ContainerArrow.Start
    else -> this
}

internal fun chooseBalloonPlacement(
    anchor: IntRect,
    viewport: IntRect,
    preferred: ContainerArrow,
    layoutDirection: LayoutDirection,
    gap: Int,
    minWidth: Int,
    minHeight: Int,
): BalloonPlacement? {
    // 在目标四侧分别切出一个矩形，保留 gap，防止箭头尖端或气泡本体贴住高亮。
    // 以下 Start/End 指经过 physical() 转换后的物理方向：
    //
    //                                anchor.left       anchor.right
    // viewport.left                  |                 |                         viewport.right
    // |<---- Start 区域 ---->|-- gap --|<---- anchor ---->|-- gap --|<---- End 区域 ---->|
    //                       ^                                    ^
    //                anchor.left - gap                    anchor.right + gap
    //
    // Start: area.right = anchor.left - gap，End: area.left = anchor.right + gap。
    // 两者纵向均覆盖 viewport.top..viewport.bottom。
    //
    // viewport.top
    //      |          Top 区域
    // anchor.top - gap -----------------
    //      |          gap
    // anchor.top       -----------------
    //      |          anchor
    // anchor.bottom    -----------------
    //      |          gap
    // anchor.bottom + gap --------------
    //      |          Bottom 区域
    // viewport.bottom
    //
    // Top/Bottom 横向均覆盖 viewport.left..viewport.right。
    // 切分边界需 coerceIn(viewport)：目标贴边或在视口外时，将无空间的一侧收拢为零，避免负宽高。
    val candidates = ContainerArrow.entries.map { direction ->
        val area = when (direction.physical(layoutDirection)) {
            ContainerArrow.Top -> viewport.copy(
                bottom = (anchor.top - gap).coerceIn(viewport.top, viewport.bottom),
            )

            ContainerArrow.Bottom -> viewport.copy(
                top = (anchor.bottom + gap).coerceIn(viewport.top, viewport.bottom),
            )

            ContainerArrow.Start -> viewport.copy(
                right = (anchor.left - gap).coerceIn(viewport.left, viewport.right),
            )

            ContainerArrow.End -> viewport.copy(
                left = (anchor.right + gap).coerceIn(viewport.left, viewport.right),
            )
        }
        BalloonPlacement(direction, area)
    }.filter { it.area.width >= minWidth && it.area.height >= minHeight }
    // 先过滤窄到无法正常显示提示的区域，再优先保留调用方方向。
    // 首选方向不可用时，按 width * height 选择剩余面积最大的一侧；用 Long 避免乘法溢出。
    // 四侧均不可用时返回 null，交给调用方显示无箭头提示，不伪造指向目标的箭头。
    return candidates.firstOrNull { it.arrow == preferred }
        ?: candidates.maxByOrNull { it.area.width.toLong() * it.area.height }
}

private val IntRect.centerX: Int get() = (left + right) / 2
private val IntRect.centerY: Int get() = (top + bottom) / 2

private fun Int.coerceWithin(min: Int, max: Int): Int = if (max < min) min else coerceIn(min, max)

@Composable
fun RevealContainer(
    steps: Int,
    field: KMutableProperty0<Boolean>,
    content: @Composable () -> Unit
) {
    var show by remember { mutableStateOf(field.get()) }
    RevealContainer(
        steps = steps,
        show = show,
        onShowDismissed = { field.set(false); show = false },
        content = content,
    )
}

@Composable
fun RevealContainer(
    steps: Int,
    show: Boolean = false,
    onShowDismissed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = rememberRevealState()
    val registry = remember(steps) {
        mutableStateMapOf<Int, RevealOverlayRegistration>().apply {
            for (i in 0..<steps) {
                put(i, RevealOverlayRegistration())
            }
        }
    }


    // registry 中的气泡和 RevealState 中的 revealable 都完成注册后，
    // 才允许调用 `reveal(0)`，避免目标刚进入 Composition 时触发竞态崩溃。
    val reallyShow = show &&
            registry[0]?.content != null &&
            state.containsRevealable(0) &&
            //fast判断
            registry.keys.size == state.revealableKeys.size &&
            registry.keys.toList().sorted() == state.revealableKeys.toList().filterIsInstance<Int>().sorted()

    // `reallyShow` 会在 Reveal 库完成布局注册后变为 true；未准备好或引导被关闭时隐藏遮罩。
    LaunchedEffect(registry, reallyShow) {
        if (reallyShow) {
            state.reveal(0)
        } else {
            state.hide()
        }
    }

    Reveal(
        revealState = state,
        overlayContent = {
            val key = it as Int
            val registration = registry[key]
            if (registration?.content != null && registration.anchorBounds != null) {
                registration.content(registration.anchorBounds)
            }
        },
        onRevealableClick = {
            val step = state.currentRevealableKey as Int
            scope.launch {
                if (step + 1 >= steps) {
                    onShowDismissed()
                    return@launch
                }
                state.reveal(step + 1)
            }
        },
        onOverlayClick = {
            val step = state.currentRevealableKey as Int
            scope.launch {
                if (step + 1 >= steps) {
                    onShowDismissed()
                    return@launch
                }
                state.reveal(step + 1)
            }
        },
    ) {
        CompositionLocalProvider(
            LocalRevealState provides state,
            LocalRevealKeyRegistry provides registry,
        ) {
            content()
        }
    }
}
