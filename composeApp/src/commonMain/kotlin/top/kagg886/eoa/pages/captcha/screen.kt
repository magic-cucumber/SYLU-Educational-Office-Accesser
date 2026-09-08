package top.kagg886.eoa.pages.captcha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold

@Serializable
data class CaptchaRoute(val data: ByteArray) {
    companion object {
        const val RESULT_KEY = "captcha_result"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CaptchaRoute

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

@Composable
fun CaptchaScreen(route: CaptchaRoute) {
    val nav = LocalNavController.current
    var captchaText by rememberSaveable { mutableStateOf("") }

    fun finish(result: String) {
        nav.previousBackStackEntry
            ?.savedStateHandle
            ?.set(CaptchaRoute.RESULT_KEY, result)
        nav.popBackStack()
    }

    DialogPageScaffold(
        title = { Text("请输入下方图片中显示的验证码") },
        confirmButton = {
            TextButton(
                enabled = captchaText.isNotBlank(),
                onClick = { finish(captchaText) }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = { finish("") }) {
                Text("取消")
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = route.data,
                contentDescription = "验证码图片",
                modifier = Modifier.height(56.dp),
                contentScale = ContentScale.FillHeight
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = captchaText,
                onValueChange = { captchaText = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("验证码") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (captchaText.isNotBlank()) {
                            finish(captchaText)
                        }
                    }
                )
            )
        }
    }
}
