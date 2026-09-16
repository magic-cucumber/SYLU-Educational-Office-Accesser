package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

actual fun Modifier.miuiLongShotSupport(
    scrollState: ScrollableState,
    enabled: Boolean,
): Modifier = composed {
    val registry = LocalLongShotTargetRegistry.current
    val target = remember(scrollState) { ScrollableStateLongShotTarget(scrollState) }
    DisposableEffect(registry, target, enabled) {
        if (enabled) registry?.register(target)
        onDispose { registry?.unregister(target) }
    }
    onGloballyPositioned { target.boundsInRoot = it.boundsInRoot() }
}
