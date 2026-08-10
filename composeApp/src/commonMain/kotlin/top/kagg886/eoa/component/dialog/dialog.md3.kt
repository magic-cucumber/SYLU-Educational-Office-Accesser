package top.kagg886.eoa.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.AlertDialogDefaults.iconContentColor
import androidx.compose.material3.AlertDialogDefaults.shape
import androidx.compose.material3.AlertDialogDefaults.textContentColor
import androidx.compose.material3.AlertDialogDefaults.titleContentColor
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.snack.EOAToaster
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.util.Platform
import top.kagg886.util.current

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 11:05
 * ================================================
 */

@Composable
fun DialogPageScaffold(
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,

    snack: ToasterState = rememberToasterState(),

    title: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null

) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

    val dialogPaneDescription = "对话框"

    CompositionLocalProvider(LocalSnackBarHost provides snack) {
        Box(
            modifier =
                modifier
                    .sizeIn(
                        minWidth = DialogMinWidth,
                        maxWidth = with(LocalDensity.current) {
                            min(DialogMaxWidth, LocalWindowInfo.current.containerSize.width.toDp() * 0.9f)
                        },
                    )
                    .applyIf(Platform.current is Platform.Desktop) { padding(vertical = 14.dp) }
                    .applyIf(Platform.current !is Platform.Desktop) { safeGesturesPadding() }
                    .imePadding()
                    .then(Modifier.semantics { paneTitle = dialogPaneDescription })
                    .clickable(enabled = true, indication = null, interactionSource = null, onClick = {}),
            propagateMinConstraints = true
        ) {
            Surface(
                shape = shape,
                color = containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(Modifier.padding(24.dp)) {
                    icon?.let {
                        CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                            Box(Modifier.align(Alignment.CenterHorizontally)) { it() }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    title?.let {
                        CompositionLocalProvider(LocalContentColor provides titleContentColor) { it() }
                        Spacer(Modifier.height(16.dp))
                    }
                    text?.let {
                        CompositionLocalProvider(LocalContentColor provides textContentColor) { it() }
                        Spacer(Modifier.height(24.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            ButtonsMainAxisSpacing,
                            Alignment.End,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }

    val model = rootViewModel()
    val rootState by model.collectAsState()

    val theme by rootState.theme.collectAsState()

    val dark =
        (theme == AppSettingsMMKVType.AppTheme.Dark) || (theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme())

    EOAToaster(
        state = snack,
        dark = dark,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp

private val ButtonsMainAxisSpacing = 8.dp
