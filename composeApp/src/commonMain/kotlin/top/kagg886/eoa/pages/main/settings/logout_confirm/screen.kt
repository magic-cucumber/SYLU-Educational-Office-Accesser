package top.kagg886.eoa.pages.main.settings.logout_confirm

import StackedSnackbarAnimation
import StackedSnackbarDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectSideEffect
import rememberStackedSnackbarHostState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.main.MainRouteViewEffect
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType.*
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object LogoutConfirmRoute

@Composable
fun LogoutConfirmScreen() {
    val model = mainViewModel()
    val nav = LocalNavController.current
    val snack = rememberStackedSnackbarHostState(animation = StackedSnackbarAnimation.Slide)
    model.collectSideEffect { effect ->
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
                    duration = StackedSnackbarDuration.Short
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


    DialogPageScaffold(
        title = { Text("退出登录") },
        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
        confirmButton = {
            TextButton(
                onClick = { model.logout() },
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { nav.popBackStack() },
            ) {
                Text("取消")
            }
        }
    ) {
        Text(
            text = "退出登录后，自定义课程将会清除，且无法回复。\n是否真的退出？",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
