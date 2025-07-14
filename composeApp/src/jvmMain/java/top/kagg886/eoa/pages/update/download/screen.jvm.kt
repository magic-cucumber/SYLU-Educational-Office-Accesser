package top.kagg886.eoa.pages.update.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalUriHandler
import top.kagg886.eoa.LocalNavController

@Composable
actual fun UpdateDownloadScreen(route: UpdateDownloadRoute) {
    val nav = LocalNavController.current
    val uri = LocalUriHandler.current
    LaunchedEffect(Unit) {
        uri.openUri(route.url)
        nav.popBackStack()
    }
}
