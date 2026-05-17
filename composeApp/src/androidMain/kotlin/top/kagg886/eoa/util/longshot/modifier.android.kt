package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.glance.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
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

    if (enabled) {
        DisposableEffect(target) {
            logger.i("ScrollState 长截屏支持已启用，fillerHeight=${target.fillerHeightDp}px")
            registry.register(target)
            onDispose {
                registry.unregister(target)
            }
        }
    } else {
        DisposableEffect(target) {
            logger.i("ScrollState 长截屏支持未启用")
            onDispose {}
        }
    }

    this
}
