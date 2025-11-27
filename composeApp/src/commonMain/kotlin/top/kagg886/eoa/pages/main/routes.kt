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
import top.kagg886.eoa.pages.logcat.LogcatRoute
import top.kagg886.eoa.pages.logcat.LogcatScreen
import top.kagg886.eoa.pages.main.settings.SettingsRoute
import top.kagg886.eoa.pages.main.settings.installSettingsGraph
import top.kagg886.eoa.pages.main.settings.list.SettingListRoute

@Serializable
data object MainRoute

val installMainGraph: NavGraphBuilder.() -> Unit = {
    composable<AboutRoute> { AboutScreen() }
    navigation<HomeRoute>(startDestination = SummaryRoute, builder = installHomeGraph)
    navigation<SettingsRoute>(startDestination = SettingListRoute, builder = installSettingsGraph)
}
