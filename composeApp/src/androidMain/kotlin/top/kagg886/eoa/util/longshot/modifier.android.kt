package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:43
 * ================================================
 */

actual fun Modifier.miuiLongShotSupport(
    scrollState: ScrollableState,
    enabled: Boolean,
): Modifier = composed {
    val registry = LocalLongShotTargetRegistry.current
    val scope = rememberCoroutineScope()

    val target = remember(scrollState, scope) {
        ScrollableStateLongShotTarget(scrollState, scope)
    }

    DisposableEffect(registry, target, enabled) {
        if (enabled) {
            logger.i("ScrollState 长截屏支持已启用")
            registry.register(target)
            onDispose {
                registry.unregister(target)
            }
        } else {
            logger.d("ScrollState 长截屏支持已禁用")
            onDispose {}
        }
    }

    this
}
