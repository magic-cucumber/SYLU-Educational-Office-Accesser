package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import top.kagg886.util.asTaggedLogger


actual fun Modifier.miuiLongShotSupport(
    scrollState: ScrollState,
    enabled: Boolean,
): Modifier = this.apply {
    "LongShot".asTaggedLogger.w("longshot api is unsupported on this platform")
}
