package top.kagg886.eoa.pages.welcome.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.GuideScaffold
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.welcome.WelcomeScreen
import top.kagg886.eoa.pages.welcome.theme.WelcomeThemeRoute

@Serializable
data object WelcomeHomeRoute

@Composable
fun WelcomeHomeScreen() {
    val nav = LocalNavController.current
    val uri = LocalUriHandler.current

    WelcomeScreen {
        GuideScaffold(
            subTitle = { Text("沈阳理工大学教务助手") },
            title = { Text("欢迎使用 EOA") },
            skipButton = {
                TextButton(onClick = { uri.openUri(BuildConfig.MESSAGE_QQ_GROUP_URL) }) {
                    Text("加入用户交流群")
                }
            },
            confirmButton = {
                Button(onClick = { nav.navigate(WelcomeThemeRoute) }) {
                    Text("继续")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FeatureItem(
                        icon = Icons.Default.DateRange,
                        title = "课程表",
                        description = "每周课表一目了然，支持导出日历",
                    )
                    FeatureItem(
                        icon = Icons.Default.Star,
                        title = "成绩查询",
                        description = "学期成绩与绩点实时掌握",
                    )
                    FeatureItem(
                        icon = Icons.Default.EmojiEvents,
                        title = "第二课堂",
                        description = "活动得分与完成进度随时可查，无需校园网",
                    )
                    FeatureItem(
                        icon = Icons.Default.Notifications,
                        title = "通知提醒",
                        description = "教务通知及时推送，不再错过",
                    )

                    FeatureItem(
                        icon = Icons.Default.Lock,
                        title = "安全清爽",
                        description = "应用无广告，教务信息不上传，数据隐私由你掌握",
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
