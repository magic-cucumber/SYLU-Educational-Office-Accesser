package top.kagg886.eoa.pages.main.settings.ai.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.settings.ai.manage.AISettingsManagerRoute
import top.kagg886.eoa.pages.rootViewModel

@Serializable
data object AISettingsSummaryRoute

@Composable
fun AISettingsSummaryScreen() = MainScreen {
    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val enableAI by rootState.enableAI.collectAsState()

    AISettingsSummaryContent(
        enableAI = enableAI,
        onEnableAIChanged = rootModel::postEnableAISetting,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AISettingsSummaryContent(
    enableAI: Boolean,
    onEnableAIChanged: (Boolean) -> Unit,
) {
    val nav = LocalNavController.current
    var showEnableAIConfirmation by remember { mutableStateOf(false) }
    var enableAIConfirmationCountdown by remember { mutableStateOf(0) }

    LaunchedEffect(showEnableAIConfirmation) {
        if (showEnableAIConfirmation) {
            for (seconds in 3 downTo 1) {
                enableAIConfirmationCountdown = seconds
                delay(1000)
            }
            enableAIConfirmationCountdown = 0
        }
    }

    if (showEnableAIConfirmation) {
        BasicAlertDialog(
            onDismissRequest = { showEnableAIConfirmation = false },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "启用 AI 功能",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "开始前，请花一点时间了解你的数据将如何被使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(28.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AIEnableFeatureRow(
                            icon = Icons.Default.Warning,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            title = "由第三方 AI 处理",
                            description = "数据将发送给你配置的 AI 服务商，它们与 EOA 毫无关联，请勿轻信来路不明的服务商。",
                        )

                        AIEnableFeatureRow(
                            icon = Icons.Default.CloudUpload,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = "每次生成会上传",
                            description = "你上传的图片、输入的文本，以及本学期的起止日期，用于生成课程数据。",
                        )

                        AIEnableFeatureRow(
                            icon = Icons.Default.Shield,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = "EOA 不存储数据",
                            description = "你的信息仅用于当次生成，EOA 自身不会保存任何内容。",
                        )

                        AIEnableFeatureRow(
                            icon = Icons.Default.ToggleOn,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = "随时可以关闭",
                            description = "关闭本对话框后，你可以随时回到本页面关掉 AI 开关。",
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        enabled = enableAIConfirmationCountdown == 0,
                        onClick = {
                            showEnableAIConfirmation = false
                            onEnableAIChanged(true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text(
                            if (enableAIConfirmationCountdown > 0) {
                                "请稍候（${enableAIConfirmationCountdown} 秒）"
                            } else {
                                "我已了解，启用 AI"
                            }
                        )
                    }

                    TextButton(onClick = { showEnableAIConfirmation = false }) {
                        Text("取消")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI设置") },
                navigationIcon = { BackIconButton() },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("启用AI") },
                supportingContent = { Text("关闭后将停用 AI 相关功能") },
                leadingContent = {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "启用AI",
                    )
                },
                trailingContent = {
                    Switch(
                        checked = enableAI,
                        onCheckedChange = { checked ->
                            if (checked) {
                                enableAIConfirmationCountdown = 3
                                showEnableAIConfirmation = true
                            } else {
                                onEnableAIChanged(false)
                            }
                        },
                    )
                },
            )

            val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            val disabledSupportingColor =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

            ListItem(
                headlineContent = { Text("模型管理") },
                supportingContent = { Text("管理 AI 模型配置") },
                leadingContent = {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "模型管理",
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "进入",
                    )
                },
                modifier = Modifier.clickable(
                    enabled = enableAI,
                    onClick = { if (enableAI) nav.navigate(AISettingsManagerRoute) }
                ),
                colors = if (enableAI) ListItemDefaults.colors() else ListItemDefaults.colors(
                    headlineColor = disabledContentColor,
                    leadingIconColor = disabledContentColor,
                    supportingColor = disabledSupportingColor,
                    trailingIconColor = disabledContentColor,
                )

            )
        }
    }
}

@Composable
private fun AIEnableFeatureRow(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    title: String,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
