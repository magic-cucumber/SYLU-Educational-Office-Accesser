package top.kagg886.eoa.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DefaultMinZoomScale = 0.5f
private const val DefaultMaxZoomScale = 2f
private const val ScaleThresholdEpsilon = 0.001f
private const val MouseWheelZoomStep = 1.05f

/**
 * State for a vertically scrollable and zoomable surface.
 *
 * [initialScale] is read by [rememberVerticalZoomState] only when the state is
 * first created. After that, [scale] is the single source of truth.
 */
@Stable
class VerticalZoomState(
    initialScale: Float = 1f,
    val minScale: Float = DefaultMinZoomScale,
    val maxScale: Float = DefaultMaxZoomScale,
) {
    init {
        require(minScale in 0f..maxScale) {
            "minScale must be non-negative and no greater than maxScale"
        }
    }

    var scale by mutableFloatStateOf(initialScale.coerceIn(minScale, maxScale))
        private set

    internal var zoomAnchorY by mutableStateOf<Float?>(null)
    internal var pendingScale by mutableStateOf<Float?>(null)
    internal var scrollCompensatedScale by mutableFloatStateOf(scale)

    internal fun zoomBy(zoomChange: Float, anchorY: Float?): Float {
        if (!zoomChange.isFinite() || zoomChange <= 0f) {
            return 1f
        }

        val targetScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        val effectiveZoomChange = targetScale / scale
        if (effectiveZoomChange == 1f) {
            return 1f
        }

        zoomAnchorY = anchorY
        pendingScale = targetScale
        scale = targetScale
        return effectiveZoomChange
    }

    internal fun finishScrollCompensation() {
        scrollCompensatedScale = scale

        val pending = pendingScale
        if (pending != null && abs(pending - scale) <= ScaleThresholdEpsilon) {
            pendingScale = null
            zoomAnchorY = null
        }
    }
}

@androidx.compose.runtime.Composable
fun rememberVerticalZoomState(
    initialScale: Float = 1f,
    minScale: Float = DefaultMinZoomScale,
    maxScale: Float = DefaultMaxZoomScale,
): VerticalZoomState = remember(minScale, maxScale) {
    VerticalZoomState(
        initialScale = initialScale,
        minScale = minScale,
        maxScale = maxScale,
    )
}

/**
 * Adds Ctrl+wheel zoom, two-finger zoom and vertical dragging to [this].
 *
 * The zoom anchor is expressed in this modifier's coordinates. [scaleOriginPx]
 * is the unscaled vertical origin of the content, used to keep the content
 * under the pointer/centroid stable while the [scrollState] is compensated.
 */
fun Modifier.verticalZoom(
    state: VerticalZoomState,
    scrollState: ScrollState,
    enabled: Boolean = true,
    scaleOriginPx: Float = 0f,
    onZoomChange: (Float) -> Unit = {},
): Modifier = composed {
    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val currentScaleState = rememberUpdatedState(state.scale)

    LaunchedEffect(state, state.scale, scrollState, scaleOriginPx) {
        val anchorY = state.zoomAnchorY
        val pendingScale = state.pendingScale
        val previousScale = state.scrollCompensatedScale
        val currentScale = state.scale

        if (anchorY != null && pendingScale != null && currentScale != previousScale) {
            val scaleChange = currentScale / previousScale
            val currentScroll = scrollState.value.toFloat()
            val targetScroll =
                scaleOriginPx + (currentScroll + anchorY - scaleOriginPx) * scaleChange - anchorY

            scrollState.scrollTo(targetScroll.roundToInt())
        }

        state.finishScrollCompensation()
    }

    fun applyZoom(zoomChange: Float, anchorY: Float?) {
        val effectiveZoomChange = state.zoomBy(zoomChange, anchorY)
        if (effectiveZoomChange != 1f) {
            currentOnZoomChange(effectiveZoomChange)
        }
    }

    this
        // Intercept Ctrl+wheel before verticalScroll consumes it.
        .pointerInput(state, enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.type != PointerEventType.Scroll || !event.keyboardModifiers.isCtrlPressed) {
                        continue
                    }

                    val delta = event.changes.firstOrNull()?.scrollDelta ?: continue
                    val wheelDelta = when {
                        delta.y != 0f -> delta.y
                        delta.x != 0f -> delta.x
                        else -> continue
                    }

                    event.changes.forEach { it.consume() }
                    if (!enabled) {
                        continue
                    }

                    val zoomFactor = if (wheelDelta < 0f) {
                        MouseWheelZoomStep
                    } else {
                        1f / MouseWheelZoomStep
                    }
                    applyZoom(
                        zoomChange = zoomFactor,
                        anchorY = event.changes.first().position.y,
                    )
                }
            }
        }
        // Consume multi-touch from Initial so cards and verticalScroll do not
        // win the gesture race. Single-finger input remains available to them.
        .pointerInput(state, enabled) {
            if (!enabled) {
                return@pointerInput
            }

            awaitPointerEventScope {
                while (true) {
                    var isMultiTouchGesture = false
                    var pastTouchSlop = false
                    var accumulatedZoom = 1f
                    var gestureScale = currentScaleState.value
                    var event = awaitPointerEvent(pass = PointerEventPass.Initial)

                    do {
                        if (event.changes.count { it.pressed } >= 2) {
                            isMultiTouchGesture = true
                        }

                        if (isMultiTouchGesture) {
                            val zoomChange = event.calculateZoom()

                            if (!pastTouchSlop) {
                                accumulatedZoom *= zoomChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                pastTouchSlop =
                                    abs(1f - accumulatedZoom) * centroidSize > viewConfiguration.touchSlop
                            }

                            event.changes.forEach { it.consume() }

                            if (pastTouchSlop && zoomChange != 1f) {
                                val targetScale =
                                    (gestureScale * zoomChange).coerceIn(state.minScale, state.maxScale)
                                val effectiveZoomChange = targetScale / gestureScale

                                if (effectiveZoomChange != 1f) {
                                    applyZoom(
                                        zoomChange = effectiveZoomChange,
                                        anchorY = event.calculateCentroid().y,
                                    )
                                    gestureScale = targetScale
                                }
                            }
                        }

                        if (event.changes.none { it.pressed }) {
                            break
                        }

                        event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    } while (true)
                }
            }
        }
        .verticalScroll(scrollState)
}

