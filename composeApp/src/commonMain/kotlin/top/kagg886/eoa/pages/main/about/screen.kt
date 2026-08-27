package top.kagg886.eoa.pages.main.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import sylu_eoa.composeapp.generated.resources.Res
import sylu_eoa.composeapp.generated.resources.good
import sylu_eoa.composeapp.generated.resources.icon
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.setText

@Serializable
data object AboutRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() = MainScreen {
    var showDonationDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val snack = LocalSnackBarHost.current
    val scope = rememberCoroutineScope()
    val beatScale = remember { Animatable(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = { BackIconButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(Res.drawable.icon),
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = beatScale.value
                        scaleY = beatScale.value
                    }
                    .clip(MaterialTheme.shapes.large)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                scope.launch {
                                    beatScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = keyframes {
                                            durationMillis = 260
                                            0.96f at 45 using FastOutLinearInEasing
                                            1.28f at 105 using FastOutLinearInEasing
                                            1.08f at 180 using LinearOutSlowInEasing
                                            1.00f at 260 using LinearOutSlowInEasing
                                        }
                                    )
                                }
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SYLU-EOA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "版本 ${BuildConfig.APP_VERSION_NAME} · 作者 kagg886",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            AboutActionItem(
                icon = Icons.Default.Favorite,
                title = "赞赏我",
                subtitle = "请我喝一杯咖啡",
                onClick = { showDonationDialog = true }
            )

            AboutActionItem(
                icon = Icons.Default.Group,
                title = "加入QQ群",
                subtitle = BuildConfig.MESSAGE_QQ_GROUP_LABEL,
                onClick = { uriHandler.openUri(BuildConfig.MESSAGE_QQ_GROUP_URL) },
                trailingContent = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboardManager.setText(BuildConfig.MESSAGE_QQ_GROUP_LABEL)
                                snack.showSnackBar(SnackBarType.Success, "已复制QQ群号")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制QQ群号"
                        )
                    }
                }
            )

            AboutActionItem(
                icon = Icons.Default.Email,
                title = "邮件反馈",
                subtitle = BuildConfig.MESSAGE_MAIL,
                onClick = {
                    uriHandler.openUri(
                        "mailto:${BuildConfig.MESSAGE_MAIL}?subject=SYLU-EOA%20%E5%8A%9F%E8%83%BD%E5%8F%8D%E9%A6%88"
                    )
                }
            )

            AboutActionItem(
                icon = Icons.Default.Code,
                title = "查看源代码",
                subtitle = "Gitee",
                onClick = {
                    uriHandler.openUri(
                        "https://${BuildConfig.MESSAGE_GITEE_HOST}/kagg886/sylu-educational-office-accesser/tree/master-4.0/"
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDonationDialog) {
        DonationDialog(onDismiss = { showDonationDialog = false })
    }
}

@Composable
private fun AboutActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        trailingContent = trailingContent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun DonationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Coffee,
                contentDescription = null
            )
        },
        title = { Text("请我喝一杯咖啡") },
        text = {
            Image(
                painter = painterResource(Res.drawable.good),
                contentDescription = "赞赏二维码",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    )
}
