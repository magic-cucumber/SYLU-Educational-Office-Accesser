package top.kagg886.eoa.pages.main.home.gym.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.home.gym.gymViewModel

@Serializable
data object GymLoginRoute

@Composable
fun GymLoginScreen() {
    val nav = LocalNavController.current
    val gymModel = gymViewModel() ?: return
    val gymState by gymModel.collectAsState()
    val result = gymState.loginRequest ?: return
    val model = viewModel { GymLoginModel(result) }
    val state by model.collectAsState()
    model.collectSideEffect {
        when (it) {
            GymLoginSideEffect.Dismiss -> nav.popBackStack()
        }
    }
    var password by remember {
        mutableStateOf(state.password)
    }
    DialogPageScaffold(
        title = { Text("请输入体育测试密码") },
        confirmButton = {
            TextButton(enabled = password.isNotBlank() && !state.submitting, onClick = { model.confirm(password) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { model.cancel() }, enabled = !state.submitting) { Text("取消") }
        }
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("由于您在系统修改过密码，我们无法为您自动获取数据。请在下面的输入框中键入密码，然后轻点“确定”。")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                enabled = !state.submitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { model.confirm(password) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
