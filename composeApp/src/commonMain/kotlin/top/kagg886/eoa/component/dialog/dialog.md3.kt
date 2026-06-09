@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialogContent
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.AlertDialogDefaults.iconContentColor
import androidx.compose.material3.AlertDialogDefaults.shape
import androidx.compose.material3.AlertDialogDefaults.textContentColor
import androidx.compose.material3.AlertDialogDefaults.titleContentColor
import androidx.compose.material3.AlertDialogFlowRow
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.DialogTokens
import androidx.compose.material3.value
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

    val dialogPaneDescription = getString(Strings.Dialog)

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
            AlertDialogContent(
                buttons = {
                    AlertDialogFlowRow(
                        mainAxisSpacing = ButtonsMainAxisSpacing,
                        crossAxisSpacing = ButtonsCrossAxisSpacing
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                },
                icon = icon,
                title = title,
                text = text,
                shape = shape,
                containerColor = containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                // Note that a button content color is provided here from the dialog's token, but in
                // most cases, TextButtons should be used for dismiss and confirm buttons.
                // TextButtons will not consume this provided content color value, and will used their
                // own defined or default colors.
                buttonContentColor = DialogTokens.ActionLabelTextColor.value,
                iconContentColor = iconContentColor,
                titleContentColor = titleContentColor,
                textContentColor = textContentColor,
            )
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
private val ButtonsCrossAxisSpacing = 12.dp
