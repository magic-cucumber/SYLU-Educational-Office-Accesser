package top.kagg886.eoa.pages.main

import androidx.compose.runtime.Composable
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.SnackBarType.*
import top.kagg886.eoa.util.showSnackBar

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/31 11:14
 * ================================================
 */

@Composable
fun MainScreen(content: @Composable () -> Unit) {
    val model = mainViewModelOrNull()
    val nav = LocalNavController.current
    val snack = LocalSnackBarHost.current
    model?.collectSideEffect { effect ->
        when (effect) {
            is MainRouteViewEffect.Toast -> {
                snack.showSnackBar(
                    type = effect.type,
                    title = when (effect.type) {
                        Success -> "成功"
                        Warning -> "警告"
                        Error -> "错误"
                        Info -> "信息"
                    },
                    description = effect.message,
                )
            }

            is MainRouteViewEffect.NavigateToLogin -> {
                nav.navigate(LoginRoute) {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    content()
}
