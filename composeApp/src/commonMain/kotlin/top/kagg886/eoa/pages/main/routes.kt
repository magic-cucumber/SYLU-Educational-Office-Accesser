package top.kagg886.eoa.pages.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.about.AboutRoute
import top.kagg886.eoa.pages.main.about.AboutScreen
import top.kagg886.eoa.pages.main.home.HomeRoute
import top.kagg886.eoa.pages.main.home.installHomeGraph
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.settings.SettingRoute
import top.kagg886.eoa.pages.main.settings.SettingScreen

@Serializable
data object MainRoute

val installMainGraph: NavGraphBuilder.() -> Unit = {
    composable<AboutRoute> { AboutScreen() }
    navigation<HomeRoute>(startDestination = SummaryRoute, builder = installHomeGraph)
    composable<SettingRoute> { SettingScreen() }
}
