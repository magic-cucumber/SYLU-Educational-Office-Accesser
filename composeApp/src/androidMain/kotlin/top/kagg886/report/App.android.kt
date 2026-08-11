package top.kagg886.report

import androidx.compose.ui.platform.UriHandler

internal actual suspend fun UriHandler.openIfSupported(url: String): Boolean =
    runCatching { openUri(url) }.isSuccess
