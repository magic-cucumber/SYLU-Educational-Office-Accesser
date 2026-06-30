package top.kagg886.eoa.pages.main.settings.ai.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.AdaptiveListItem
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.ai.edit.AISettingsEditRoute
import top.kagg886.eoa.util.SnackBarType

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 17:51
 * ================================================
 */


@Serializable
data object AISettingsListRoute

@Composable
fun AISettingsScreen() = MainScreen {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AISettingsContent(
    state: AISettingsListState,
    onDelete: (LLMProviderEntity) -> Unit,
) {
    val nav = LocalNavController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型管理") },
                navigationIcon = { BackIconButton() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(AISettingsEditRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                    overlineContent = { Text(provider.modelName) },
                                    supportingContent = {
                                        Text(
                                            listOfNotNull(
                                                if (provider.supportMultimodal) "支持多模态" else null,
                                                if (provider.supportNativeJsonOutput) "支持原生JSON" else null,
                                            ).joinToString(" / ")
                                        )
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
