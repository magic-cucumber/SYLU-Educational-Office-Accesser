package top.kagg886.eoa.pages.welcome.collect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.GuideScaffold
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.pages.welcome.WelcomeScreen
import top.kagg886.eoa.pages.welcome.done.WelcomeDoneRoute


@Serializable
data object WelcomeCollectRoute

@Composable
fun WelcomeCollectScreen() {
    val nav = LocalNavController.current
    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val enableCrashReport by rootState.enableCrashReport.collectAsState()
    val enableAI by rootState.enableAI.collectAsState()

    WelcomeScreen {
        GuideScaffold(
            subTitle = { Text("你的数据由你掌控") },
            title = { Text("数据收集授权") },
            backButton = { BackIconButton() },
            confirmButton = {
                Button(
                    onClick = {
                        rootModel.postEnableCrashReportSetting(true)
                        rootModel.postEnableAISetting(true)
                        nav.navigate(WelcomeDoneRoute)
                    }
                ) {
                    Text("全部同意")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            skipButton = {
                TextButton(onClick = { nav.navigate(WelcomeDoneRoute) }) {
                    Text("按现有设置")
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = "以下功能涉及数据收集或上传，均为可选，关闭后不影响核心功能的使用。\n您可以选择全部同意，也可以勾选您希望与我们共享的数据后按现有设置授权我们收集数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("崩溃报告") },
                    supportingContent = {
                        Text("应用崩溃时，自动收集崩溃日志、应用设置与设备信息并加密上传，用于定位与修复问题。账号、密码等凭据会被脱敏，不会被收集。")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "崩溃报告"
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = enableCrashReport,
                            onCheckedChange = rootModel::postEnableCrashReportSetting
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("AI 功能") },
                    supportingContent = {
                        Text("开启后可使用 AI 相关功能。使用时，相关数据将直接发送给你自行配置的模型服务商，EOA 不会经过或存储这些数据。")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "AI 功能"
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = enableAI,
                            onCheckedChange = rootModel::postEnableAISetting
                        )
                    }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "之后可随时在「设置 → 高级」与「设置 → 模型管理」中更改。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
