package top.kagg886.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

@ExperimentalComposeUiApi
actual suspend fun Clipboard.setText(text: String) = setClipEntry(
    ClipEntry.withPlainText(text),
)
