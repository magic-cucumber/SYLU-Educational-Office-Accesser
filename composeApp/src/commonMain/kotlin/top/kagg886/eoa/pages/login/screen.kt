package top.kagg886.eoa.pages.login

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

@Serializable
data object LoginRoute

@Composable
fun LoginScreen() {
    val model = viewModel { LoginViewModel() }
    val state by model.collectAsState()

    val nav = LocalNavController.current
    val snack = LocalSnackBarHost.current
    model.collectSideEffect {
        when (it) {
            is LoginSideEffect.NavigateToMain -> {
                nav.navigate(MainRoute) {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            is LoginSideEffect.Toast -> {
                snack.showSnackBar(
                    it.type,
                    it.message
                )
            }
        }
    }

    val uri = LocalUriHandler.current
    LoginScreenContent(
        state = state,
        onLoginButtonClicked = { username, password ->
            model.startLogin(username, password)
        },
        onForgetPasswordButtonClicked = {
            uri.openUri("https://jxw.sylu.edu.cn/pwdmgr/retake/index.zf")
        },
        onVerifyCodeInput = {
            model.processVerifyCode(it.ifBlank { null })
        },
        onLoginBackendChanged = {
            model.setLoginClient(it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    state: LoginViewModelState,
    onLoginBackendChanged: (EOAClientProvider) -> Unit,
    onLoginButtonClicked: (String, String) -> Unit,
    onForgetPasswordButtonClicked: () -> Unit,
    onVerifyCodeInput: (String) -> Unit
) = when (state) {
    is LoginViewModelState.WaitLogin -> {
        var username by remember { mutableStateOf(AppLoginPropertiesMMKV.username) }
        var password by remember { mutableStateOf(AppLoginPropertiesMMKV.password) }
        var passwordVisible by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current

        if (state is LoginViewModelState.WaitLogin.VerifyCode) {
            var captchaText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {
                    onVerifyCodeInput("")
                },
                title = { Text("请输入验证码") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("请输入下方图片中显示的验证码")
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = captchaText,
                                onValueChange = { captchaText = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                label = { Text("验证码") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (captchaText.isNotBlank()) {
                                        onVerifyCodeInput(captchaText)
                                    }
                                })
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Display the captcha image with same height as the text field
                            AsyncImage(
                                model = state.data,
                                contentDescription = "验证码图片",
                                modifier = Modifier.height(56.dp),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (captchaText.isNotBlank()) {
                                onVerifyCodeInput(captchaText)
                            }
                        }
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onVerifyCodeInput("") }
                    ) {
                        Text("取消")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "登录到教务网",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("教务网账号") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "用户名") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(
                        FocusDirection.Down
                    )
                }),
                enabled = state !is LoginViewModelState.WaitLogin.Processing
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("教务网密码") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "密码") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (username.isNotBlank() && password.isNotBlank() && state !is LoginViewModelState.WaitLogin.Processing) {
                        onLoginButtonClicked(username, password)
                    }
                }),
                enabled = state !is LoginViewModelState.WaitLogin.Processing
            )

            Spacer(modifier = Modifier.height(16.dp))
            //TODO 实现登录后端选择。
            AnimatedVisibility(
                visible = state is LoginViewModelState.WaitLogin.Waiting
            ) {
                if (state is LoginViewModelState.WaitLogin.Waiting) {
                    var expanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "选择登录后端",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state.selected.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                label = { Text("后端选择") }
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                state.provider.forEach { provider ->
                                    DropdownMenuItem(
                                        text = {
                                            ListItem(
                                                headlineContent = { Text(provider.name) },
                                                supportingContent = {
                                                    Column {
                                                        Text(provider.description)
                                                        Text(
                                                            text = "版本: ${provider.version}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                colors = ListItemDefaults.colors(
                                                    containerColor = MenuDefaults.containerColor
                                                )
                                            )
                                        },
                                        onClick = {
                                            onLoginBackendChanged(provider)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Login button
                Button(
                    onClick = { onLoginButtonClicked(username, password) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = username.isNotBlank() && password.isNotBlank() && state !is LoginViewModelState.WaitLogin.Processing,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            (fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down)).togetherWith(
                                fadeOut() + slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Down
                                )
                            )
                        }
                    ) {
                        when (val loginState = it) {
                            is LoginViewModelState.WaitLogin.Processing -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(loginState.toast)
                                }
                            }

                            else -> {
                                Text(
                                    "登录",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Forgot password button
                OutlinedButton(
                    onClick = onForgetPasswordButtonClicked,
                    modifier = Modifier
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("找回密码")
                }
            }

            // Fix height jitter by using a fixed height spacer
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    LoginViewModelState.Empty -> Unit
}
