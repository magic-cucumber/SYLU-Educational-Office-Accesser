package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Whether the provider is currently generating a long screenshot. */
val LocalLongShotEnabled = compositionLocalOf<Boolean> {
    error("you should enable long shot")
}

expect fun Modifier.miuiLongShotSupport(
    scrollState: ScrollableState,
    enabled: Boolean = true,
): Modifier
