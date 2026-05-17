package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier


expect fun Modifier.miuiLongShotSupport(
    scrollState: ScrollState,
    enabled: Boolean = true,
): Modifier
