package top.kagg886.eoa.pages.main.home.gym

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import top.kagg886.eoa.component.nav.transition
import top.kagg886.eoa.pages.main.home.gym.list.GymListRoute
import top.kagg886.eoa.pages.main.home.gym.list.GymListScreen
import top.kagg886.eoa.pages.main.home.gym.list.filter.GymListFilterRoute
import top.kagg886.eoa.pages.main.home.gym.list.filter.GymListFilterScreen
import top.kagg886.eoa.pages.main.home.gym.login.GymLoginRoute
import top.kagg886.eoa.pages.main.home.gym.login.GymLoginScreen

val installGymGraph: NavGraphBuilder.() -> Unit = {
    transition<GymListRoute>(deepLinks = listOf(navDeepLink<GymListRoute>(basePath = "eoa://home/gym"))) {
        GymListScreen()
    }
    dialog<GymListFilterRoute> { GymListFilterScreen() }
    dialog<GymLoginRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { GymLoginScreen() }
}
