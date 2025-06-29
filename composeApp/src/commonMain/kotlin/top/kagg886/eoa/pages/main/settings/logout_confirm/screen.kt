package top.kagg886.eoa.pages.main.settings.logout_confirm

import StackedSnackbarAnimation
import StackedSnackbarDuration
import StackedSnackbarHost
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectSideEffect
import rememberStackedSnackbarHostState
import top.kagg886.eoa.LocalNavController
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

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.5f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "是否退出登录？",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // 内容
                Text(
                    text = "退出登录后，自定义课程将会清除，且无法回复。\n是否真的退出？",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    OutlinedButton(
                        onClick = { nav.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }

                    // 确认按钮
                    Button(
                        onClick = { model.logout() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确认")
                    }
                }
            }
        }

        StackedSnackbarHost(
            hostState = snack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
