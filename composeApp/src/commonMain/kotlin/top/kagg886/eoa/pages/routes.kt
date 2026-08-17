package top.kagg886.eoa.pages

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.announcement.AnnouncementRoute
import top.kagg886.eoa.pages.announcement.AnnouncementScreen
import top.kagg886.eoa.pages.logcat.LogcatRoute
import top.kagg886.eoa.pages.logcat.LogcatScreen
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.login.LoginScreen
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.home.HomeRoute
import top.kagg886.eoa.pages.main.installMainGraph
import top.kagg886.eoa.pages.update.UpdateRoute
import top.kagg886.eoa.pages.update.detail.UpdateDetailRoute
import top.kagg886.eoa.pages.update.installUpdateGraph
import top.kagg886.eoa.pages.welcome.WelcomeRoute
import top.kagg886.eoa.pages.welcome.home.WelcomeHomeRoute
import top.kagg886.eoa.pages.welcome.installWelcomeGraph

@Serializable
data object RootRoute

val installEOAGraph: (NavGraphBuilder.() -> Unit) = {
    navigation<RootRoute>(startDestination = WelcomeRoute) {
        navigation<WelcomeRoute>(startDestination = WelcomeHomeRoute, builder = installWelcomeGraph)
        composable<LoginRoute> { LoginScreen() }
        navigation<UpdateRoute>(startDestination = UpdateDetailRoute("", "", ""), builder = installUpdateGraph)
        dialog<AnnouncementRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
            AnnouncementScreen(
                it.toRoute()
            )
        }
        navigation<MainRoute>(startDestination = HomeRoute, builder = installMainGraph)
        composable<LogcatRoute> { LogcatScreen() }
    }
}
