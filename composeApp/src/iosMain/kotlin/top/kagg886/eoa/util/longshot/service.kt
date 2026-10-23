package top.kagg886.eoa.util.longshot

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.*
import platform.Foundation.NSData
import top.kagg886.util.asTaggedLogger
import kotlin.time.Duration.Companion.seconds

private var activeLongShotBridge: LongShotBridge? = null

/** Kotlin entry point used by the Swift UIScreenshotService delegate. */
public class LongShotBridge internal constructor(
    private val scope: CoroutineScope,
    private val controller: LongShotController,
    private val onLongShotEnabledChange: (Boolean) -> Unit,
) {
    private var job: Job? = null
    private var disposed = false

    public fun generatePdf(completion: (NSData?) -> Unit) {
        fun complete(data: NSData?) {
            completion(data)
        }

        if (disposed || job?.isActive == true) {
            complete(null)
            return
        }

        // Start immediately so cancellation also reaches the completion finally block.
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var data: NSData? = null
            try {
                onLongShotEnabledChange(true)
                data = withTimeout(3.seconds) {
                    // Let consumers apply their screenshot layout before reading its bounds.
                    withFrameNanos { }
                    controller.generate()
                }
            } catch (_: CancellationException) {
                // Cancellation is reported as a failed PDF, but must still complete UIKit.
            } catch (e: Exception) {
                "LongShot".asTaggedLogger.w(e) { "iOS 整页截图失败" }
            } finally {
                onLongShotEnabledChange(false)
                try {
                    // Keep UIKit's snapshot overlay until scroll restoration and sticky text
                    // have reached a new frame, including on timeout or cancellation.
                    withContext(NonCancellable) {
                        withTimeoutOrNull(1.seconds) { controller.awaitRestoredFrame() }
                    }
                } catch (e: Exception) {
                    "LongShot".asTaggedLogger.w(e) { "iOS 截图后画面刷新失败" }
                } finally {
                    job = null
                    complete(data)
                }
            }
        }
    }

    public fun dispose() {
        disposed = true
        job?.cancel()
        job = null
        if (activeLongShotBridge === this) activeLongShotBridge = null
    }
}

/** Returns the bridge owned by the currently composed iOS root. */
public fun currentLongShotBridge(): LongShotBridge? = activeLongShotBridge

internal fun installLongShotBridge(bridge: LongShotBridge) {
    activeLongShotBridge = bridge
}
