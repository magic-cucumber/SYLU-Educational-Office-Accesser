@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.MutatePriority
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

internal class LongShotController(
    private val root: LongShotRoot,
    private val registry: LongShotTargetRegistry,
    private val view: UIView,
) {
    suspend fun generate(): NSData? {
        val target = registry.target() ?: return null
        val size = root.size
        val rootBounds = root.bounds
        val bounds = target.boundsInRoot
        val scene = view.window?.windowScene ?: return null
        val orientation = scene.interfaceOrientation
        val viewSize = view.bounds.useContents { this.size.width to this.size.height }
        check(size.width > 0 && size.height > 0 && viewSize.first > 0)
        val top = (bounds.top - rootBounds.top).roundToInt().coerceIn(0, size.height)
        val bottom = (bounds.bottom - rootBounds.top).roundToInt().coerceIn(top, size.height)
        val viewport = bottom - top
        check(viewport > 0)
        fun validate() {
            check(root.size == size && root.bounds == rootBounds && target.boundsInRoot == bounds) {
                "Layout changed during capture"
            }
            // The registry's preferred target can change as lazy/pager content composes.
            // This transaction keeps the target chosen when the screenshot began.
            check(registry.contains(target)) { "Screenshot target was removed during capture" }
            check(view.window?.windowScene == scene) { "Window scene changed during capture" }
            check(scene.interfaceOrientation == orientation) { "Orientation changed during capture" }
            check(view.bounds.useContents { this.size.width to this.size.height } == viewSize) {
                "Container size changed during capture"
            }
        }
        val strips = mutableListOf<PdfStrip>()
        var encodedBytes = 0L
        var height = 0
        fun append(frame: ImageBitmap, start: Int, count: Int) {
            if (count == 0) return
            check(height.toLong() + count <= 100_000) { "Page is too tall" }
            val bytes = encodeStrip(frame, start, count)
            encodedBytes += bytes.size
            check(encodedBytes <= 64L * 1024 * 1024) { "Screenshot memory limit reached" }
            strips += PdfStrip(bytes, count)
            height += count
        }
        target.state.scroll(MutatePriority.PreventUserInput) {
            var displacement = 0f
            suspend fun move(delta: Float): Float {
                currentCoroutineContext().ensureActive()
                val consumed = scrollBy(delta)
                check(consumed.isFinite())
                displacement += consumed
                return consumed
            }
            try {
                var steps = 0
                while (target.state.canScrollBackward) {
                    validate()
                    check(++steps <= 2_000) { "Cannot reach page top" }
                    check(move(-viewport.toFloat()) < -0.01f) { "Scroll made no progress" }
                }
                var frame = root.capture()
                validate()
                check(frame.width == size.width && frame.height == size.height)
                append(frame, 0, top)
                append(frame, top, viewport)
                var distance = 0.0
                var emitted = 0
                steps = 0
                while (target.state.canScrollForward) {
                    validate()
                    check(++steps <= 2_000)
                    val consumed = move(viewport.toFloat())
                    check(consumed > 0.01f && consumed <= viewport + 0.01f) { "Invalid scroll distance" }
                    distance += consumed
                    // Round the cumulative distance, avoiding a one-pixel error per strip.
                    val next = distance.roundToInt()
                    val count = next - emitted
                    check(count in 0..viewport)
                    frame = root.capture()
                    validate()
                    append(frame, bottom - count, count)
                    emitted = next
                }
                append(frame, bottom, size.height - bottom)
            } finally {
                // Undo even a partially completed rewind, while still holding the scroll mutex.
                withContext(NonCancellable) {
                    withTimeoutOrNull(3.seconds) {
                        var attempts = 0
                        while (abs(displacement) > 0.01f && ++attempts <= 2_000) {
                            val consumed = scrollBy((-displacement).coerceIn(-viewport.toFloat(), viewport.toFloat()))
                            if (!consumed.isFinite() || abs(consumed) < 0.01f) break
                            displacement += consumed
                            yield()
                        }
                    }
                }
            }
        }
        currentCoroutineContext().ensureActive()
        validate()
        return renderPdf(strips, size.width, height, viewSize.first)
    }
}

private data class PdfStrip(val png: ByteArray, val height: Int)

private fun encodeStrip(frame: ImageBitmap, top: Int, height: Int): ByteArray {
    val cropped = ImageBitmap(frame.width, height)
    try {
        Canvas(cropped).drawImageRect(
            frame, IntOffset(0, top), IntSize(frame.width, height),
            IntOffset.Zero, IntSize(frame.width, height), Paint(),
        )
        val image = Image.makeFromBitmap(cropped.asSkiaBitmap())
        try {
            val data = checkNotNull(image.encodeToData(EncodedImageFormat.PNG))
            try { return data.bytes } finally { data.close() }
        } finally {
            image.close()
        }
    } finally {
        cropped.asSkiaBitmap().close()
    }
}

private fun renderPdf(strips: List<PdfStrip>, width: Int, height: Int, pointWidth: Double): NSData {
    val scale = pointWidth / width
    val renderer = UIGraphicsPDFRenderer(
        CGRectMake(0.0, 0.0, pointWidth, height * scale), UIGraphicsPDFRendererFormat(),
    )
    return renderer.PDFDataWithActions { context ->
        checkNotNull(context).beginPage()
        var y = 0
        strips.forEach { strip ->
            autoreleasepool {
                val data = strip.png.usePinned {
                    NSData.create(bytes = it.addressOf(0), length = strip.png.size.toULong())
                }
                val image = checkNotNull(UIImage(data = data))
                image.drawInRect(CGRectMake(0.0, y * scale, pointWidth, strip.height * scale))
            }
            y += strip.height
        }
    }
}
