package top.kagg886.report

import androidx.compose.ui.platform.UriHandler
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL.Companion.URLWithString
import platform.UIKit.UIApplication
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun UriHandler.openIfSupported(url: String): Boolean =
    suspendCancellableCoroutine { continuation ->
        val nsUrl = URLWithString(url)
        if (nsUrl == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = { success ->
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }
        )
    }
