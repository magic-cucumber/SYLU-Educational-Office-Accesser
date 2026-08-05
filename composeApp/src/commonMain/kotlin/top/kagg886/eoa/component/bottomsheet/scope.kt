package top.kagg886.eoa.component.bottomsheet

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/8/5 22:19
 * ================================================
 */

@Stable
interface BottomSheetPageScaffoldScope {
    /** Makes this layout match the part of the sheet that is currently visible. */
    fun Modifier.matchContent(): Modifier
}

internal class BottomSheetPageScaffoldScopeImpl(
    private val minimumContentHeight: Int,
    private val visibleContentHeight: () -> Int
) : BottomSheetPageScaffoldScope {
    override fun Modifier.matchContent(): Modifier = layout { measurable, constraints ->
        val height = visibleContentHeight()
            .coerceAtLeast(minimumContentHeight)
            .coerceIn(
                minimumValue = constraints.minHeight,
                maximumValue = constraints.maxHeight
            )
        val placeable = measurable.measure(
            constraints.copy(minHeight = height, maxHeight = height)
        )
        layout(placeable.width, height) {
            placeable.placeRelative(0, 0)
        }
    }
}
