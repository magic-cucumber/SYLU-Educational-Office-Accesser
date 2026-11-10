package top.kagg886.eoa.pages.main.home.gym

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.pages.main.home.gym.list.filter.GymListFilterRoute
import top.kagg886.eoa.pages.main.home.gym.login.GymLoginRoute
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object GymRoute

@Composable
fun GymScreen(content: @Composable () -> Unit) {
    val nav = LocalNavController.current
    val toast = LocalSnackBarHost.current
    val model = gymViewModel() ?: return
    model.collectSideEffect {
        when (it) {
            GymSideEffect.NavigateToLogin -> nav.navigate(GymLoginRoute) { launchSingleTop = true }
            GymSideEffect.NavigateToFilter -> nav.navigate(GymListFilterRoute) { launchSingleTop = true }
            is GymSideEffect.Toast -> toast.showSnackBar(it.level, it.message)
        }
    }
    content()
}
