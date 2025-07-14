package top.kagg886.eoa.pages.update

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.update.detail.UpdateDetailRoute
import top.kagg886.eoa.pages.update.detail.UpdateScreen
import top.kagg886.eoa.pages.update.download.UpdateDownloadRoute
import top.kagg886.eoa.pages.update.download.UpdateDownloadScreen

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/14 14:02
 * ================================================
 */

@Serializable
data object UpdateRoute

val installUpdateGraph: NavGraphBuilder.() -> Unit = {
    dialog<UpdateDetailRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { UpdateScreen(it.toRoute()) }
    dialog<UpdateDownloadRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        UpdateDownloadScreen(
            it.toRoute()
        )
    }
}
