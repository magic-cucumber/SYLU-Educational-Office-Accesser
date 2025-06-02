package top.kagg886.eoa.pages.main.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import sylu_eoa.composeapp.generated.resources.Res
import sylu_eoa.composeapp.generated.resources.good
import sylu_eoa.composeapp.generated.resources.icon
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import kotlinx.coroutines.launch
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton

@Serializable
data object AboutRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    var showDonationDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("关于") },
            navigationIcon = { BackIconButton() }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                // 项目图标
                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(120.dp)
                )
            }

            item {
                // 项目信息
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SYLU-EOA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "制作者：kagg886",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "版本：4.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // 赞赏我
                ListItem(
                    headlineContent = { Text("赞赏我") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDonationDialog = true
                        }
                )
            }

            item {
                // 加入QQ群
                ListItem(
                    headlineContent = { Text("加入QQ群") },
                    supportingContent = {
                        val snack = LocalSnackBarHost.current
                        val scope = rememberCoroutineScope()
                        val theme = MaterialTheme.colorScheme
                        Text(
                            buildAnnotatedString {
                                withLink(
                                    link = LinkAnnotation.Clickable(
                                        tag = "jq_number",
                                        styles = TextLinkStyles(
                                            style = androidx.compose.ui.text.SpanStyle(
                                                color = theme.primary,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            ),
                                            pressedStyle = androidx.compose.ui.text.SpanStyle(
                                                color = theme.primary.copy(alpha = 0.8f),
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            ),
                                            hoveredStyle = androidx.compose.ui.text.SpanStyle(
                                                color = theme.primary.copy(alpha = 0.9f),
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            )
                                        )
                                    ) {
                                        clipboardManager.setText(
                                            buildAnnotatedString {
                                                append("798201505")
                                            }
                                        )
                                        scope.launch {
                                            snack.showSuccessSnackbar("已复制QQ群号")
                                        }
                                    },
                                    block = {
                                        append("点我复制QQ群号")
                                    }
                                )
                            }
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uriHandler.openUri("https://qm.qq.com/q/heTEDas3Mk")
                        }
                )
            }

            item {
                // 查看源代码
                ListItem(
                    headlineContent = { Text("查看源代码") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uriHandler.openUri("https://gitee.com/kagg886/sylu-educational-office-accesser/tree/master-4.0/")
                        }
                )
            }
        }
    }


    // 赞赏对话框
    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDonationDialog = false
                    }
                ) {
                    Text("关闭")
                }
            },
            icon = {
                Icon(
                    Icons.Default.Coffee,
                    contentDescription = null,
                    tint = Color.Red
                )
            },
            title = { Text("请我喝1杯咖啡") },
            text = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.good),
                        contentDescription = "Donation QR Code",
                        modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        )
    }
}
