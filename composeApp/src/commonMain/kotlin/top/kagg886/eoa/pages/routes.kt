package top.kagg886.eoa.pages

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.login.LoginScreen
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.home.HomeRoute
import top.kagg886.eoa.pages.main.installMainGraph
import top.kagg886.eoa.pages.welcome.WelcomeRoute
import top.kagg886.eoa.pages.welcome.WelcomeScreen

val installEOAGraph: (NavGraphBuilder.() -> Unit) = {
    composable<WelcomeRoute> { WelcomeScreen() }
    composable<LoginRoute> { LoginScreen() }
    navigation<MainRoute>(startDestination = HomeRoute, builder = installMainGraph)
}
