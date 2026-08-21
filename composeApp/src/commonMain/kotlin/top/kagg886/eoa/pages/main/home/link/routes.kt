package top.kagg886.eoa.pages.main.home.link

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import kotlinx.serialization.Serializable
import top.kagg886.eoa.component.nav.transition
import top.kagg886.eoa.pages.main.home.link.list.LinkListRoute
import top.kagg886.eoa.pages.main.home.link.list.LinkListScreen
import top.kagg886.eoa.pages.main.home.link.tips.LinkTipsRoute
import top.kagg886.eoa.pages.main.home.link.tips.LinkTipsScreen

@Serializable
data object LinkRoute

val installLinkGraph: NavGraphBuilder.() -> Unit = {
    transition<LinkListRoute> { LinkListScreen() }
    dialog<LinkTipsRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { LinkTipsScreen() }
}
