package top.kagg886.eoa.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled,onBack)
