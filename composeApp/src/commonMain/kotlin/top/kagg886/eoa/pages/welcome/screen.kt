package top.kagg886.eoa.pages.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.main.MainRoute

@Composable
fun WelcomeScreen(content: @Composable () -> Unit) {
    val model = welcomeModelOrNull() ?: return
    val nav = LocalNavController.current
    val uri = LocalUriHandler.current

    model.collectSideEffect {
        when (it) {
            WelcomeSideEffect.NavigateToLogin -> {
                nav.navigate(LoginRoute) {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            is WelcomeSideEffect.NavigateToMain -> {
                nav.navigate(MainRoute) {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            is WelcomeSideEffect.NavigateToURL -> {
                uri.openUri(it.url)
            }
        }
    }
    val state by model.collectAsState()
    when (state) {
        is WelcomeViewModelState.Empty -> return
        else -> content()
    }
}
