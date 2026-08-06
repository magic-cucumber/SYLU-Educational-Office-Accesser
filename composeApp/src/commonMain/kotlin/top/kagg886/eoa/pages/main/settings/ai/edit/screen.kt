package top.kagg886.eoa.pages.main.settings.ai.edit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.bottomsheet.BottomSheetPageScaffold
import top.kagg886.eoa.component.bottomsheet.SheetPosition
import top.kagg886.eoa.pages.main.mainViewModelOrNull


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
        when (it) {
            is LLMProviderEditSideEffect.Toast -> mainModel.toast(it.type, it.message)
            LLMProviderEditSideEffect.NavigateBack -> nav.popBackStack()
        }
    }
    val state by model.collectAsState()

    BottomSheetPageScaffold(
        maxExpandedHeight = LocalWindowInfo.current.containerDpSize.height * 0.8f,
        initialPopupType = SheetPosition.Expanded,
        popupTypeChangeRequest = {
            if (it != SheetPosition.Hidden) return@BottomSheetPageScaffold true
            val state = state as? LLMProviderEditState.Success ?: return@BottomSheetPageScaffold true
            return@BottomSheetPageScaffold !state.confirming
        }
    ) {
        when (val current = state) {
            LLMProviderEditState.Loading -> {
                Column(Modifier.matchContent()) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            BackIconButton(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close"
                                    )
                                },
                                onBackPressed = {
                                    close()
                                },
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BottomSheetDefaults.ContainerColor
                        )
                    )

                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }

            is LLMProviderEditState.Success -> {
                LLMProviderEditPage(
                    modifier = Modifier.matchContent(),
                    initial = current.provider,
                    confirming = current.confirming,
                    onDismiss = { close() },
                    onConfirm = { model.save(it) }
                )
            }
        }
    }
}

@Composable
private fun LLMProviderEditPage(
    modifier: Modifier = Modifier,
    initial: LLMProviderEntity,
    confirming: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LLMProviderEntity) -> Unit
) {
    var modelName by remember(initial.uuid) { mutableStateOf(initial.modelName) }
    var modelKey by remember(initial.uuid) { mutableStateOf(initial.modelKey) }
    var baseUrl by remember(initial.uuid) { mutableStateOf(initial.baseUrl) }
    var modelRemark by remember(initial.uuid) { mutableStateOf(initial.modelRemark) }
    var modelDescription by remember(initial.uuid) { mutableStateOf(initial.modelDescription) }

    Column(modifier) {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text(if (initial.uuid.isBlank()) "添加AI模型" else "编辑AI模型") },
            navigationIcon = {
                BackIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    },
                    onBackPressed = {
                        onDismiss()
                    },
                    enabled = !confirming,
                )
            },
            actions = {
                IconButton(
                    enabled = modelName.isNotBlank() && modelKey.isNotBlank() && baseUrl.isNotBlank() && !confirming,
                    onClick = {
                        onConfirm(
                            initial.copy(
                                modelName = modelName,
                                modelKey = modelKey,
                                baseUrl = baseUrl,
                                modelRemark = modelRemark,
                                modelDescription = modelDescription,
                            )
                        )
                    }
                ) {
                    AnimatedContent(
                        targetState = confirming,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        }
                    ) { confirming ->
                        if (confirming) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                            )
                        }
                    }
                }

            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BottomSheetDefaults.ContainerColor
            )
        )

        LLMProviderEditForm(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            enabled = !confirming,
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
        )
    }
}

@Composable
private fun LLMProviderEditForm(
    modifier: Modifier = Modifier,
    modelRemark: String,
    enabled: Boolean,
    onModelRemarkChange: (String) -> Unit,
    modelDescription: String,
    onModelDescriptionChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    modelKey: String,
    onModelKeyChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = modelRemark,
            onValueChange = onModelRemarkChange,
            label = { Text("模型备注") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            enabled = enabled,
        )
        OutlinedTextField(
            value = modelDescription,
            onValueChange = onModelDescriptionChange,
            label = { Text("模型简述") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            enabled = enabled,
        )
        OutlinedTextField(
            value = modelName,
            onValueChange = onModelNameChange,
            label = { Text("模型名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            enabled = enabled,
        )
        OutlinedTextField(
            value = modelKey,
            onValueChange = onModelKeyChange,
            label = { Text("模型Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            enabled = enabled,
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            enabled = enabled,
        )
    }
}
