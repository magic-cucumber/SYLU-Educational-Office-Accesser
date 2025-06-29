package top.kagg886.eoa.util

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

@Deprecated("it's action is can't be predicted")
fun <T : Any> NavHostController.replace(route: T, builder: NavOptionsBuilder.() -> Unit = {}) {
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute != null) {
        popBackStack()
        navigate(route) {
            builder()
        }
    } else {
        navigate(route) {
            builder()
        }
    }
}
