package top.kagg886.eoa.pages

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.login.LoginScreen
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.home.HomeRoute
import top.kagg886.eoa.pages.main.installMainGraph
import top.kagg886.eoa.pages.update.UpdateRoute
import top.kagg886.eoa.pages.update.UpdateScreen
import top.kagg886.eoa.pages.welcome.WelcomeRoute
import top.kagg886.eoa.pages.welcome.WelcomeScreen

@Serializable
data object RootRoute

val installEOAGraph: (NavGraphBuilder.() -> Unit) = {
    navigation<RootRoute>(startDestination = WelcomeRoute) {
        composable<WelcomeRoute> { WelcomeScreen() }
        composable<LoginRoute> { LoginScreen() }
        dialog<UpdateRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { UpdateScreen(it.toRoute()) }
        navigation<MainRoute>(startDestination = HomeRoute, builder = installMainGraph)
    }
}
