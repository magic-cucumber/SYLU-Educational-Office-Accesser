@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/** Owns the Compose capture surface and publishes its Swift screenshot bridge. */
@Composable
fun ProvideLongShot(content: @Composable () -> Unit) {
    val viewController = LocalUIViewController.current
    val layer = rememberGraphicsLayer()
    val root = remember(layer) { LongShotRoot(layer) }
    val registry = remember { LongShotTargetRegistry() }
    val scope = rememberCoroutineScope()
    DisposableEffect(root, registry, viewController) {
        val bridge = LongShotBridge(
            scope, LongShotController(root, registry, viewController.view),
        )
        installLongShotBridge(bridge)
        onDispose {
            bridge.dispose()
        }
    }
    CompositionLocalProvider(LocalLongShotTargetRegistry provides registry) {
        Box(Modifier.fillMaxSize().onGloballyPositioned {
            root.bounds = it.boundsInRoot()
            root.size = it.size
        }.drawWithContent {
            val request = root.request
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            root.completed.value = request
        }) { content() }
    }
}

internal class LongShotRoot(val layer: GraphicsLayer) {
    var bounds = Rect.Zero
    var size = IntSize.Zero
    var request by mutableIntStateOf(0)
    val completed = MutableStateFlow(-1)

    suspend fun capture() = withTimeout(3.seconds) {
        // Let scroll-triggered layout run, then explicitly invalidate our capture layer.
        withFrameNanos { }
        val generation = ++request
        completed.first { it >= generation }
        layer.toImageBitmap()
    }
}
