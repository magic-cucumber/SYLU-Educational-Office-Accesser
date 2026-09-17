package top.kagg886.eoa.pages.main.settings.ai.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                        onCheckedChange = onEnableAIChanged,
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
