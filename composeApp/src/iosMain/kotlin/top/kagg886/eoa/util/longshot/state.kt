package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

internal val LocalLongShotTargetRegistry = staticCompositionLocalOf<LongShotTargetRegistry?> { null }

internal class ScrollableStateLongShotTarget(val state: ScrollableState) {
    var boundsInRoot: Rect = Rect.Zero
}

internal class LongShotTargetRegistry {
    private val targets = mutableListOf<ScrollableStateLongShotTarget>()
    fun register(target: ScrollableStateLongShotTarget) { targets.add(target) }
    fun unregister(target: ScrollableStateLongShotTarget) { targets.remove(target) }
    fun contains(target: ScrollableStateLongShotTarget): Boolean = targets.any { it === target }
    // Pager precomposition may register off-screen pages. Only capture a visible target.
    fun target(): ScrollableStateLongShotTarget? = targets
        .filter { it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0 }
        .maxByOrNull { it.boundsInRoot.width * it.boundsInRoot.height }
}
