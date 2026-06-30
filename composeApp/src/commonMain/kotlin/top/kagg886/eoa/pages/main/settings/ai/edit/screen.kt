package top.kagg886.eoa.pages.main.settings.ai.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.SnackBarType


@Serializable
data class AISettingsEditRoute(
    val uuid: String? = null,
)


@Composable
fun AISettingsEditScreen(route: AISettingsEditRoute) {
    val nav = LocalNavController.current
    val mainModel = mainViewModelOrNull() ?: return
    val model = viewModel(key = route.toString()) {
        LLMProviderEditModel(mainModel.database, route.uuid)
    }
    model.collectSideEffect {
        when(it) {
            is LLMProviderEditSideEffect.Toast -> mainModel.toast(SnackBarType.Success,it.message)
            LLMProviderEditSideEffect.NavigateBack -> nav.popBackStack()
        }
    }
    val state by model.collectAsState()

    when (val current = state) {
        LLMProviderEditState.Loading -> {
            DialogPageScaffold(
                title = { Text("编辑AI模型") },
                icon = { Icon(Icons.Default.Psychology, contentDescription = "模型管理") },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { nav.popBackStack() }) {
                        Text("返回")
                    }
                }
            ) {
                Text("正在加载中，请稍等。")
            }
        }

        is LLMProviderEditState.Success -> {
            LLMProviderEditPage(
                initial = current.provider,
                confirming = current.confirming,
                onDismiss = { nav.popBackStack() },
                onConfirm = { model.save(it) }
            )
        }
    }
}

@Composable
private fun LLMProviderEditPage(
    initial: LLMProviderEntity,
    confirming: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LLMProviderEntity) -> Unit,
) {
    var modelName by remember(initial.uuid) { mutableStateOf(initial.modelName) }
    var modelKey by remember(initial.uuid) { mutableStateOf(initial.modelKey) }
    var baseUrl by remember(initial.uuid) { mutableStateOf(initial.baseUrl) }
    var supportMultimodal by remember(initial.uuid) { mutableStateOf(initial.supportMultimodal) }
    var supportNativeJsonOutput by remember(initial.uuid) { mutableStateOf(initial.supportNativeJsonOutput) }
    var modelRemark by remember(initial.uuid) { mutableStateOf(initial.modelRemark) }
    var modelDescription by remember(initial.uuid) { mutableStateOf(initial.modelDescription) }

    DialogPageScaffold(
        icon = { Icon(Icons.Default.Psychology, contentDescription = "模型管理") },
        title = { Text(if (initial.uuid.isBlank()) "添加AI模型" else "编辑AI模型") },
        confirmButton = {
            TextButton(
                enabled = modelName.isNotBlank() && modelKey.isNotBlank() && !confirming,
                onClick = {
                    onConfirm(
                        initial.copy(
                            modelName = modelName,
                            modelKey = modelKey,
                            baseUrl = baseUrl,
                            supportMultimodal = supportMultimodal,
                            supportNativeJsonOutput = supportNativeJsonOutput,
                            modelRemark = modelRemark,
                            modelDescription = modelDescription,
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        LLMProviderEditForm(
            modelRemark = modelRemark,
            onModelRemarkChange = { modelRemark = it },
            modelDescription = modelDescription,
            onModelDescriptionChange = { modelDescription = it },
            modelName = modelName,
            onModelNameChange = { modelName = it },
            modelKey = modelKey,
            onModelKeyChange = { modelKey = it },
            baseUrl = baseUrl,
            onBaseUrlChange = { baseUrl = it },
            supportMultimodal = supportMultimodal,
            onSupportMultimodalChange = { supportMultimodal = it },
            supportNativeJsonOutput = supportNativeJsonOutput,
            onSupportNativeJsonOutputChange = { supportNativeJsonOutput = it },
        )
    }
}

@Composable
private fun LLMProviderEditForm(
    modelRemark: String,
    onModelRemarkChange: (String) -> Unit,
    modelDescription: String,
    onModelDescriptionChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    modelKey: String,
    onModelKeyChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    supportMultimodal: Boolean,
    onSupportMultimodalChange: (Boolean) -> Unit,
    supportNativeJsonOutput: Boolean,
    onSupportNativeJsonOutputChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = modelRemark,
            onValueChange = onModelRemarkChange,
            label = { Text("模型备注") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        OutlinedTextField(
            value = modelDescription,
            onValueChange = onModelDescriptionChange,
            label = { Text("模型简述") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        OutlinedTextField(
            value = modelName,
            onValueChange = onModelNameChange,
            label = { Text("模型名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        OutlinedTextField(
            value = modelKey,
            onValueChange = onModelKeyChange,
            label = { Text("模型Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        ListItem(
            headlineContent = { Text("支持多模态") },
            trailingContent = {
                Switch(
                    checked = supportMultimodal,
                    onCheckedChange = onSupportMultimodalChange
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = AlertDialogDefaults.containerColor,
            )
        )
        ListItem(
            headlineContent = { Text("支持原生JSON输出") },
            trailingContent = {
                Switch(
                    checked = supportNativeJsonOutput,
                    onCheckedChange = onSupportNativeJsonOutputChange
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = AlertDialogDefaults.containerColor,
            )
        )
    }
}
