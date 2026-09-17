package top.kagg886.eoa.pages.main.settings.ai.manage.edit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
data class AISettingsManagerEditRoute(
    val uuid: String? = null,
)


@Composable
fun AISettingsManagerEditScreen(route: AISettingsManagerEditRoute) {
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
                        windowInsets = WindowInsets(),
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
            windowInsets = WindowInsets(),
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
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DataNoticeBanner()

        SectionHeader(text = "连接信息")

        var baseUrlVisited by remember { mutableStateOf(false) }
        val baseUrlError = baseUrlVisited && baseUrl.isBlank()
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL") },
            placeholder = { Text("https://api.openai.com/v1") },
            supportingText = { Text(if (baseUrlError) "此项必填" else "需兼容 OpenAI 接口") },
            isError = baseUrlError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (!it.isFocused) baseUrlVisited = true },
            enabled = enabled,
        )

        var keyVisited by remember { mutableStateOf(false) }
        var keyVisible by remember { mutableStateOf(false) }
        val keyError = keyVisited && modelKey.isBlank()
        OutlinedTextField(
            value = modelKey,
            onValueChange = onModelKeyChange,
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            supportingText = if (keyError) {
                { Text("此项必填") }
            } else {
                null
            },
            isError = keyError,
            trailingIcon = {
                IconButton(
                    onClick = { keyVisible = !keyVisible },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (keyVisible) "隐藏Key" else "显示Key"
                    )
                }
            },
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (!it.isFocused) keyVisited = true },
            enabled = enabled,
        )

        var nameVisited by remember { mutableStateOf(false) }
        val nameError = nameVisited && modelName.isBlank()
        OutlinedTextField(
            value = modelName,
            onValueChange = onModelNameChange,
            label = { Text("模型名称") },
            placeholder = { Text("例如 gpt-4o") },
            supportingText = { Text(if (nameError) "此项必填" else "将用作请求的 model 参数") },
            isError = nameError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (!it.isFocused) nameVisited = true },
            enabled = enabled,
        )

        SectionHeader(text = "展示信息")

        OutlinedTextField(
            value = modelRemark,
            onValueChange = onModelRemarkChange,
            label = { Text("备注（可选）") },
            placeholder = { Text("例如：主力模型") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )

        OutlinedTextField(
            value = modelDescription,
            onValueChange = onModelDescriptionChange,
            label = { Text("简述（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DataNoticeBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "保存时会向对应提供商发送测试数据，以验证多模态等能力是否可用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}
