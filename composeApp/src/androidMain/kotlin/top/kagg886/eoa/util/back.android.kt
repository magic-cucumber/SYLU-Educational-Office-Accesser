package top.kagg886.eoa.util

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled,onBack)
