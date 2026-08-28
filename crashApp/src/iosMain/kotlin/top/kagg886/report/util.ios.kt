package top.kagg886.report

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setText(text: String) = this.setClipEntry(ClipEntry.withPlainText(text))