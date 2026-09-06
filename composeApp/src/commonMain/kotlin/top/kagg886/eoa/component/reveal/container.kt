package top.kagg886.eoa.component.reveal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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

private val LocalRevealScope = staticCompositionLocalOf<RevealScope> {
    error("CompositionLocal LocalRevealScope not provided")
}

private val LocalRevealKeyRegistry = staticCompositionLocalOf<MutableMap<Int, RevealOverlayRegistration>> {
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


fun Modifier.revealableAutoMeasured(step: Int, arrow: ContainerArrow, content: @Composable BoxScope.() -> Unit) =
    composed {
        val registry = LocalRevealKeyRegistry.current
        val scope = LocalRevealScope.current

        DisposableEffect(content, scope, registry, arrow) {
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
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
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

        with(scope) {
            Modifier
                .onGloballyPositioned {
                    val position = it.positionInRoot()
                    val left = position.x.roundToInt()
                    val top = position.y.roundToInt()
                    registry[step] = registry.getValue(step).copy(
                        anchorBounds = IntRect(
                            left = left,
                            top = top,
                            right = left + it.size.width,
                            bottom = top + it.size.height,
                        )
                    )
                }
                .revealable(
                    key = step,
                    shape = RevealShape.RoundRect(8.dp)
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

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Balloon(
                modifier = Modifier.padding(8.dp),
                arrow = arrow.toBalloonArrow(
                    horizontalAlignment = horizontalArrowAlignment,
                    verticalAlignment = verticalArrowAlignment,
                ),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = 2.dp,
                content = content
            )
        }
    ) { measurables, constraints ->
        // 测量阶段只关心两个范围：
        //
        // 横向：
        // 0              screenMargin                    screenWidth - screenMargin       screenWidth
        // |--------------------|====================================|--------------------|
        // |<-- screenMargin -->|<---------- maxChildWidth --------->|<-- screenMargin -->|
        //
        // 纵向：
        // 0              screenMargin                   screenHeight - screenMargin      screenHeight
        // |--------------------|====================================|--------------------|
        // |<-- screenMargin -->|<--------- maxChildHeight --------->|<-- screenMargin -->|
        //
        // 所以气泡最大尺寸是：
        // maxChildWidth  = screenWidth  - screenMargin * 2
        // maxChildHeight = screenHeight - screenMargin * 2
        val screenMargin = 8.dp.roundToPx()
        val maxChildWidth = (constraints.maxWidth - screenMargin * 2).coerceAtLeast(0)
        val maxChildHeight = (constraints.maxHeight - screenMargin * 2).coerceAtLeast(0)
        val childConstraints = Constraints(
            minWidth = 0,
            maxWidth = maxChildWidth,
            minHeight = 0,
            maxHeight = maxChildHeight,
        )
        val placeable = measurables.first().measure(childConstraints)
        val position = calculateBalloonPosition(
            anchorBounds = anchorBounds,
            arrow = arrow,
            childWidth = placeable.width,
            childHeight = placeable.height,
            screenWidth = constraints.maxWidth,
            screenHeight = constraints.maxHeight,
            screenMargin = screenMargin,
            layoutDirection = layoutDirection,
        )
        // Balloon 外层有 Modifier.padding(screenMargin)，placeable 的坐标包含这圈空白。
        // Arrow 的 Alignment 在气泡内容坐标内计算，所以要减掉 position + screenMargin。
        //
        // 横向：
        // 0                      position.x                  position.x + childWidth
        // |--------------------------|===================================|
        //                            |<---------- childWidth ----------->|
        //                            |-- screenMargin --|<-- Arrow 坐标空间
        // anchorBounds.left          anchorBounds.centerX          anchorBounds.right
        // |-------------------------------|------------------------------|
        // |<------- anchorBounds.centerX = (anchorBounds.left + anchorBounds.right) / 2
        //
        // horizontalArrowAlignment.center =
        //     anchorBounds.centerX - position.x - screenMargin
        //
        // 纵向同理：
        // verticalArrowAlignment.center =
        //     anchorBounds.centerY - position.y - screenMargin
        horizontalArrowAlignment.center = anchorBounds.centerX - position.x - screenMargin
        verticalArrowAlignment.center = anchorBounds.centerY - position.y - screenMargin

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(position.x, position.y)
        }
    }
}

private class MeasuredHorizontalAlignment : Alignment.Horizontal {
    var center: Int = 0

    override fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int =
        (center - size / 2).coerceWithin(0, space - size)
}

private class MeasuredVerticalAlignment : Alignment.Vertical {
    var center: Int = 0

    override fun align(size: Int, space: Int): Int =
        (center - size / 2).coerceWithin(0, space - size)
}

private fun calculateBalloonPosition(
    anchorBounds: IntRect,
    arrow: ContainerArrow,
    childWidth: Int,
    childHeight: Int,
    screenWidth: Int,
    screenHeight: Int,
    screenMargin: Int,
    layoutDirection: LayoutDirection,
): IntOffset {
    // Start/End 是逻辑方向，先换成物理方向，后面的几何计算只处理 left/right。
    val physicalArrow = when (arrow) {
        ContainerArrow.Start -> if (layoutDirection == LayoutDirection.Ltr) ContainerArrow.Start else ContainerArrow.End
        ContainerArrow.End -> if (layoutDirection == LayoutDirection.Ltr) ContainerArrow.End else ContainerArrow.Start
        else -> arrow
    }

    // 首选位置 preferredX/preferredY：
    //
    // Top / Bottom 横向居中：
    // preferredX                 anchorBounds.centerX                 preferredX + childWidth
    // |-----------------------------------|-----------------------------------|
    // |<------------ childWidth / 2 ------|------ childWidth / 2 ------------>|
    // preferredX = anchorBounds.centerX - childWidth / 2
    //
    // Start：
    // preferredX          preferredX + childWidth == anchorBounds.left        anchorBounds.right
    // |---------------------------- childWidth ----------------------------->|---- anchor ----|
    // preferredX = anchorBounds.left - childWidth
    //
    // End：
    // anchorBounds.left        anchorBounds.right == preferredX          preferredX + childWidth
    // |---------- anchor ----------|---------------------------- childWidth ------------------>|
    // preferredX = anchorBounds.right
    //
    // 纵向对应：
    // Top:    preferredY = anchorBounds.top - childHeight
    // Bottom: preferredY = anchorBounds.bottom
    // Start/End: preferredY = anchorBounds.centerY - childHeight / 2
    val preferredX = when (physicalArrow) {
        ContainerArrow.Start -> anchorBounds.left - childWidth
        ContainerArrow.End -> anchorBounds.right
        ContainerArrow.Top,
        ContainerArrow.Bottom -> anchorBounds.centerX - childWidth / 2
    }
    val preferredY = when (physicalArrow) {
        ContainerArrow.Top -> anchorBounds.top - childHeight
        ContainerArrow.Bottom -> anchorBounds.bottom
        ContainerArrow.Start,
        ContainerArrow.End -> anchorBounds.centerY - childHeight / 2
    }

    // 最终位置 position.x / position.y：
    //
    // x 可用范围：
    // screenMargin                 screenWidth - screenMargin - childWidth          screenWidth - screenMargin
    // |------------------------------------------|---------------------------------------------|
    // |<------ position.x 最小值 ---------------->|<------ position.x 最大值 ------------------>|
    //                                            |<--------------- childWidth ----------------->|
    //
    // y 可用范围：
    // screenMargin                screenHeight - screenMargin - childHeight        screenHeight - screenMargin
    // |------------------------------------------|---------------------------------------------|
    // |<------ position.y 最小值 ---------------->|<------ position.y 最大值 ------------------>|
    //                                            |<-------------- childHeight ----------------->|
    //
    // preferredX/Y 可能越界，所以用 coerceWithin 把气泡约束到屏幕以内。
    return IntOffset(
        x = preferredX.coerceWithin(screenMargin, screenWidth - screenMargin - childWidth),
        y = preferredY.coerceWithin(screenMargin, screenHeight - screenMargin - childHeight),
    )
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


    // `registry[0].content` 只表示自定义引导气泡已经注册；Reveal 库内部的
    // revealable 则要等目标完成布局后才会注册到 `RevealState`。两者都满足时，
    // 才允许调用 `reveal(0)`，避免在目标刚进入 Composition 时触发竞态崩溃。
    val reallyShow = show &&
        registry[0]?.content != null &&
        state.containsRevealable(0)

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
            LocalRevealScope provides this,
            LocalRevealKeyRegistry provides registry,
        ) {
            content()
        }
    }
}
