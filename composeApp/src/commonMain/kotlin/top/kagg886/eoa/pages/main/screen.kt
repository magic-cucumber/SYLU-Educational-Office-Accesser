package top.kagg886.eoa.pages.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.dokar.sonner.TextToastAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.captcha.CaptchaRoute
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

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun MainScreen(content: @Composable () -> Unit) {
    val model = mainViewModelOrNull()
    val nav = LocalNavController.current
    val snack = LocalSnackBarHost.current
    val uri = LocalUriHandler.current

    var defer by remember {
        mutableStateOf<CompletableDeferred<String?>?>(null)
    }
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

            is MainRouteViewEffect.SyncErrorToast -> {
                snack.showSnackBar(
                    type = Error,
                    title = "错误",
                    description = "同步失败！点按 \"帮助\"查询解决方案。",
                    actionTitle = "帮助",
                    action = {
                        uri.openUri("${BuildConfig.MESSAGE_WEBSITE_URL}/bug-report.html")
                    }
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

            is MainRouteViewEffect.NavigateToCaptcha -> {
                defer = effect.deferred
                nav.navigate(CaptchaRoute(effect.byte))
            }
        }
    }

    //验证码监听
    LaunchedEffect(nav, defer) {
        val pending = defer ?: return@LaunchedEffect

        nav.currentBackStackEntryFlow
            .map { entry ->
                entry.takeIf { current ->
                    current.destination.hierarchy.any {
                        it.hasRoute<MainRoute>()
                    }
                }
            }
            .distinctUntilChanged()
            .flatMapLatest { entry ->
                entry
                    ?.savedStateHandle
                    ?.getStateFlow<String?>(CaptchaRoute.RESULT_KEY, null)
                    ?.mapNotNull { result ->
                        result?.let { entry to it }
                    }

                    ?: emptyFlow()
            }
            .collect { (entry, result) ->
                entry.savedStateHandle
                    .remove<String>(CaptchaRoute.RESULT_KEY)

                pending.complete(result.ifBlank { null })
            }
    }


    content()
}
