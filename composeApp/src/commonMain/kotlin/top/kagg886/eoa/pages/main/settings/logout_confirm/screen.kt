package top.kagg886.eoa.pages.main.settings.logout_confirm

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.mainViewModelOrNull

@Serializable
data object LogoutConfirmRoute

@Composable
fun LogoutConfirmScreen() = MainScreen {
    val model = mainViewModelOrNull()
    val nav = LocalNavController.current
    DialogPageScaffold(
        title = { Text("退出登录") },
        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
        confirmButton = {
            TextButton(
                onClick = { model?.logout() },
            ) {
                Text("确认")
            }
        },
        snack = LocalSnackBarHost.current,
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
