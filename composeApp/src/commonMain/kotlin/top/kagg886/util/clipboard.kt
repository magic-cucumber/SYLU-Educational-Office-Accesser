package top.kagg886.util

import androidx.compose.ui.platform.Clipboard

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/30 09:22
 * ================================================
 */

expect suspend fun Clipboard.setText(text: String)
