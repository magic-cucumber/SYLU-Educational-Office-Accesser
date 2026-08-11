package top.kagg886.report

import androidx.compose.ui.platform.UriHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun UriHandler.openIfSupported(url: String): Boolean = withContext(Dispatchers.IO) {
    runCatching { openUri(url) }.isSuccess
}
