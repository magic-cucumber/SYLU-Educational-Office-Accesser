package top.kagg886.eoa.component.dropdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.window.PopupProperties

private val AccessibilityServiceEnabled = mutableStateOf(false)

/**
 * Returns the state of whether any accessibility services are enabled.
 *
 * @param listenToTouchExplorationState whether to track the enabled/disabled state of touch
 * exploration (i.e. TalkBack)
 * @param listenToSwitchAccessState whether to track the enabled/disabled state of Switch Access
 */
@Composable
internal fun rememberAccessibilityServiceState(
    listenToTouchExplorationState: Boolean = true,
    listenToSwitchAccessState: Boolean = true,
): State<Boolean> = AccessibilityServiceEnabled


internal actual class WindowBoundsCalculator(private val windowInfo: WindowInfo) {
    actual fun getVisibleWindowBounds(): IntRect = windowInfo.containerSize.toIntRect()
}

@Composable
internal actual fun platformWindowBoundsCalculator(): WindowBoundsCalculator {
    val windowInfo = LocalWindowInfo.current
    return remember(windowInfo) { WindowBoundsCalculator(windowInfo) }
}

@Composable
internal actual fun OnPlatformWindowBoundsChange(block: () -> Unit) {
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.containerSize }
            .collect { block() }
    }
}

@Composable
internal actual fun popupPropertiesForAnchorType(
    anchorType: ExposedDropdownMenuAnchorType,
    alwaysFocusable: Boolean,
): PopupProperties {
    val a11yServicesEnabled by rememberAccessibilityServiceState()

    // If typing on the IME is required, the menu should not be focusable
    // in order to prevent stealing focus from the input method.
    val imeRequired =
        anchorType == ExposedDropdownMenuAnchorType.PrimaryEditable ||
                (anchorType ==ExposedDropdownMenuAnchorType.SecondaryEditable && !a11yServicesEnabled)
    return PopupProperties(
        focusable = !imeRequired || alwaysFocusable
    )
}
