package top.kagg886.eoa.pages.welcome.done

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import sylu_eoa.composeapp.generated.resources.Res
import sylu_eoa.composeapp.generated.resources.good
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.GuideScaffold
import top.kagg886.eoa.pages.welcome.WelcomeScreen
import top.kagg886.eoa.pages.welcome.welcomeModelOrNull

private const val EOA_WEBSITE_URL = "https://eoa.kagg886.top/"
private const val EOA_QQ_GROUP_URL = "https://qm.qq.com/q/heTEDas3Mk"
private const val EOA_SOURCE_CODE_URL = "https://github.com/kagg886/SYLU-Educational-Office-Accesser/"

@Serializable
data object WelcomeDoneRoute

@Composable
fun WelcomeDoneScreen() {
    WelcomeScreen {
        val model = welcomeModelOrNull()
        val uri = LocalUriHandler.current
        var showDonationDialog by remember { mutableStateOf(false) }

        GuideScaffold(
            subTitle = { Text("即将就绪") },
            title = { Text("欢迎加入 EOA") },
            backButton = { BackIconButton() },
            skipButton = {
                TextButton(
                    enabled = model != null,
                    onClick = { model?.completeWelcomeWithoutTutorial() },
                ) {
                    Text("跳过教程并使用")
                }
            },
            confirmButton = {
                Button(
                    enabled = model != null,
                    onClick = { model?.completeWelcome() },
                ) {
                    Text("开始使用")
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
            ) {
                Spacer(Modifier.height(8.dp))

                val url = "https://jxw.sylu.edu.cn/"
                val linkStyle = TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )

                Text(
                    text = buildAnnotatedString {
                        append("准备好您的")
                        withLink(link = LinkAnnotation.Url(url,linkStyle)) {
                            append("教务网")
                        }
                        append("账号密码，我们马上出发！")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "想了解更多信息，可以前往：",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinkItem(
                        icon = Icons.Default.Language,
                        title = "前往官网",
                        description = "查阅使用文档与常见问题",
                        onClick = { uri.openUri(EOA_WEBSITE_URL) },
                    )
                    LinkItem(
                        icon = Icons.Default.Groups,
                        title = "前往 QQ 群",
                        description = "加入用户交流群，反馈问题与建议",
                        onClick = { uri.openUri(EOA_QQ_GROUP_URL) },
                    )
                    LinkItem(
                        icon = Icons.Default.Code,
                        title = "前往源代码仓库",
                        description = "查看开源代码，参与贡献",
                        onClick = { uri.openUri(EOA_SOURCE_CODE_URL) },
                    )
                    LinkItem(
                        icon = Icons.Default.Favorite,
                        title = "捐赠作者",
                        description = "请我喝一杯咖啡，支持项目持续更新",
                        onClick = { showDonationDialog = true },
                    )
                }
            }
        }

        // 捐赠对话框
        if (showDonationDialog) {
            AlertDialog(
                onDismissRequest = { showDonationDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = { showDonationDialog = false },
                    ) {
                        Text("关闭")
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        tint = Color.Red,
                    )
                },
                title = { Text("请我喝1杯咖啡") },
                text = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.good),
                            contentDescription = "捐赠二维码",
                            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                            contentScale = ContentScale.Fit,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun LinkItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
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
        Column(Modifier.weight(1f)) {
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
