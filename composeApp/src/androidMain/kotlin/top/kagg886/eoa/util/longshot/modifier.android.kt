package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:43
 * ================================================
 */

private val logger = "LongShot".asTaggedLogger

actual fun Modifier.miuiLongShotSupport(
    scrollState: ScrollState,
    enabled: Boolean,
): Modifier = composed {
    val registry = LocalLongShotTargetRegistry.current
    val scope = rememberCoroutineScope()

    val target = remember(scrollState, scope,scrollState.maxValue) {
        ScrollStateLongShotTarget(scrollState, scope, scrollState.maxValue)
    }

    DisposableEffect(registry, target, enabled) {
        if (enabled) {
            logger.i("ScrollState 长截屏支持已启用，fillerHeight=${target.fillerHeightDp}px")
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
