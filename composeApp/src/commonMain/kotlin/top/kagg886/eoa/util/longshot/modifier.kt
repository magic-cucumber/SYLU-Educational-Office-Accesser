package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier


expect fun Modifier.miuiLongShotSupport(
    scrollState: ScrollableState,
    enabled: Boolean = true,
): Modifier
