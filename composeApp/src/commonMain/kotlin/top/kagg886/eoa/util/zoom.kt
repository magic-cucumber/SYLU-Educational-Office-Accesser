package top.kagg886.eoa.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DefaultMinZoomScale = 0.5f
private const val DefaultMaxZoomScale = 2f
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

    /** Updates an inactive surface without emitting a gesture or retaining its anchor. */
    fun snapTo(scale: Float) {
        if (!scale.isFinite()) {
            return
        }

        this.scale = scale.coerceIn(minScale, maxScale)
        zoomAnchorY = null
        pendingScale = null
        scrollCompensatedScale = this.scale
    }

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

    internal fun finishScrollCompensation(appliedScale: Float) {
        scrollCompensatedScale = appliedScale

        val pending = pendingScale
        // A newer gesture may already be pending while an older composition is measured.
        // Only acknowledge the scale whose content has actually been measured.
        if (pending == appliedScale) {
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
 * [contentScale] must be the same captured scale used to size the content.
 */
fun Modifier.verticalZoom(
    state: VerticalZoomState,
    scrollState: ScrollState,
    enabled: Boolean = true,
    scaleOriginPx: Float = 0f,
    contentScale: Float = state.scale,
    onZoomChange: (Float) -> Unit = {},
): Modifier = composed {
    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val currentScaleState = rememberUpdatedState(state.scale)

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
        .layout { measurable, constraints ->
            // Read the old offset BEFORE verticalScroll measures and clamps it to the
            // new range. Ordinary scrolling must not invalidate measurement here.
            val (previousScale, previousOffset, anchorY) = Snapshot.withoutReadObservation {
                Triple(
                    state.scrollCompensatedScale,
                    scrollState.value,
                    state.zoomAnchorY.takeIf { state.pendingScale != null },
                )
            }
            val targetOffset = if (anchorY != null && contentScale != previousScale) {
                scaleOriginPx + (previousOffset + anchorY - scaleOriginPx) *
                    (contentScale / previousScale) - anchorY
            } else {
                null
            }

            // The inner verticalScroll now establishes the range for contentScale.
            val placeable = measurable.measure(constraints)
            Snapshot.withoutReadObservation {
                if (targetOffset != null) {
                    val target = targetOffset.roundToInt().coerceIn(0, scrollState.maxValue)
                    // Synchronous layout correction: placement below must see the new
                    // offset in this same pass, without a coroutine or another frame.
                    scrollState.dispatchRawDelta((target - scrollState.value).toFloat())
                }
                state.finishScrollCompensation(contentScale)
            }
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }
        .verticalScroll(scrollState)
}

