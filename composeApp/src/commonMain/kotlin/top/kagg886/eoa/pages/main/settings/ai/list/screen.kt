package top.kagg886.eoa.pages.main.settings.ai.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.AdaptiveListItem
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.ai.edit.AISettingsEditRoute
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.currentLayoutType

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 17:51
 * ================================================
 */


@Serializable
data object AISettingsListRoute

@Composable
fun AISettingsScreen() = RevealContainer(3, AppInitializeMMKV::tutorialAISettings) {
    MainScreen {
        val mainModel = mainViewModelOrNull() ?: return@MainScreen
        val model = viewModel {
            AISettingsListModel(mainModel.database)
        }
        val state by model.collectAsState()
        model.collectSideEffect {
            when (it) {
                is AISettingsListSideEffect.Toast -> mainModel.toast(SnackBarType.Success, it.message)
            }
        }

        AISettingsContent(
            state = state,
            onDelete = model::delete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AISettingsContent(
    state: AISettingsListState,
    onDelete: (LLMProviderEntity) -> Unit,
) {
    val nav = LocalNavController.current
    val fabArrow = when (currentLayoutType()) {
        NavigationSuiteType.NavigationBar -> ContainerArrow.Top
        else -> ContainerArrow.Bottom
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型管理") },
                navigationIcon = { BackIconButton() },
                actions = {
                    val uri = LocalUriHandler.current
                    IconButton(
                        onClick = {
                            uri.openUri("https://eoa.kagg886.top/settings.html#ai%E6%A8%A1%E5%9E%8B%E9%85%8D%E7%BD%AE")
                        },
                        modifier = Modifier.revealableAutoMeasured(2, ContainerArrow.Bottom) {
                            Text("如果您没有合适的模型，点击这里获取更多帮助")
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "帮助")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate(AISettingsEditRoute()) },
                modifier = Modifier.revealableAutoMeasured(1, fabArrow) {
                    Text("点这里添加一个 AI 模型。")
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .revealableAutoMeasured(0, ContainerArrow.Top) {
                    Text(
                        if (currentLayoutType() == NavigationSuiteType.NavigationBar) {
                            "这里会显示已经保存的模型。左右滑动模型，可以编辑或删除配置。"
                        } else {
                            "这里会显示已经保存的模型。点击铅笔按钮编辑，点击垃圾桶按钮删除。"
                        }
                    )
                }
        ) {
            when (state) {
                AISettingsListState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }

                is AISettingsListState.Success -> {
                    if (state.providers.isEmpty()) {
                        ErrorPage(
                            title = { Text("暂无AI配置") },
                            message = { Text("点击右下角添加模型配置") },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.providers, key = { it.uuid }) { provider ->
                                AdaptiveListItem(
                                    headlineContent = { Text(provider.modelRemark.ifBlank { provider.modelName }) },
                                    supportingContent = {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (provider.supportMultimodal) {
                                                Icon(
                                                    Icons.Default.Image,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(with(LocalDensity.current) { LocalTextStyle.current.toSpanStyle().fontSize.toDp() }),
                                                    tint = LocalTextStyle.current.toSpanStyle().color
                                                )
                                            }

                                            if (provider.supportNativeJsonOutput) {
                                                Icon(
                                                    Icons.Default.Code,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(with(LocalDensity.current) { LocalTextStyle.current.toSpanStyle().fontSize.toDp() }),
                                                    tint = LocalTextStyle.current.toSpanStyle().color
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    primaryAction {
                                        clickable { nav.navigate(AISettingsEditRoute(provider.uuid)) }
                                        icon { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                                    }
                                    secondAction {
                                        clickable { onDelete(provider) }
                                        icon { Icon(Icons.Default.Delete, contentDescription = "删除") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
