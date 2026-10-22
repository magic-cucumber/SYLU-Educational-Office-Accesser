package top.kagg886.eoa.util

import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavOptionsDsl
import androidx.navigation.NavUri
import androidx.navigation.navOptions

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


fun NavHostController.handleDeepLink(uri: String) {
    val request = NavDeepLinkRequest.Builder
        .fromUri(NavUri(uri))
        .build()

    if (!graph.hasDeepLink(request)) return

    navigate(request, navOptions = navOptions {
        popUpTo(graph.id) {
            inclusive = true
        }
        launchSingleTop = true
    })
}
