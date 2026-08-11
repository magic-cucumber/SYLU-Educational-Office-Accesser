package top.kagg886.report

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.main.home.course.list.asNoConflict
import top.kagg886.eoa.theme.AppTheme
import top.kagg886.util.Platform
import top.kagg886.util.current
import top.kagg886.util.setText
import kotlin.random.Random

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/11 16:09
 * ================================================
 */

private const val FEEDBACK_QQ_GROUP_URL = "https://qm.qq.com/q/heTEDas3Mk"
private const val FEEDBACK_EMAIL = "iveour@163.com"

@Composable
fun CrashApp(error: String, onRestart: () -> Unit) {
    val random = remember {
        Random(error.hashCode()).nextInt(36000) / 100.0f
    }
    val color = when((AppSettingsMMKV.theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme()) || AppSettingsMMKV.theme == AppSettingsMMKVType.AppTheme.Dark) {
        true -> Color.hsv(
            hue = random,
            saturation = 0.1412f,
            value = 1f
        )
        false -> Color.hsv(
            hue = random,
            saturation = 0.3038f,
            value = 0.3039f
        )
    }
    AppTheme(color = color, nightTheme = isSystemInDarkTheme()) {
        val snack = remember { SnackbarHostState() }
        val uriHandler = LocalUriHandler.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()

        Scaffold(snackbarHost = { SnackbarHost(snack) }) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "哎呀，应用崩溃了",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "很抱歉给您带来不便。如果这个问题反复出现，欢迎通过以下方式向我们反馈，帮助我们改进应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (!uriHandler.openIfSupported(buildFeedbackMailUri(error))) {
                                snack.showSnackbar("未检测到可用的邮件应用")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("邮件反馈")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            clipboard.setText(error)
                            snack.showSnackbar("崩溃日志已复制，请在群内粘贴反馈")
                            uriHandler.openUri(FEEDBACK_QQ_GROUP_URL)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("复制日志并加群反馈")
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("重启应用")
                }
            }
        }
    }
}

private fun buildFeedbackMailUri(error: String): String {
    val body = """
        <h3>SYLU-EOA 崩溃反馈</h3>
        <p><b>崩溃前操作简述：</b></p>
        <p>（请在这里简要描述崩溃发生前都做了些什么，例如：打开课表 &rarr; 点击同步 &rarr; 应用崩溃）</p>
        <hr/>
        <p><b>应用版本：</b>${BuildConfig.APP_VERSION_NAME} (${BuildConfig.APP_VERSION_CODE})</p>
        <p><b>运行平台：</b>${Platform.current}</p>
        <p><b>崩溃日志：</b></p>
        <pre>${error.escapeHtml()}</pre>
    """.trimIndent()
    val subject = "SYLU-EOA 崩溃反馈"
    return "mailto:$FEEDBACK_EMAIL?subject=${subject.encodeURLParameter()}&body=${body.encodeURLParameter()}"
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

internal expect suspend fun UriHandler.openIfSupported(url: String): Boolean
