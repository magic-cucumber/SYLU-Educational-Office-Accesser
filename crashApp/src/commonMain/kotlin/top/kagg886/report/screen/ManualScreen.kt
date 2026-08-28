package top.kagg886.report.screen

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.report.setText

/**
 * 手动反馈页：提供复制日志与邮件发送两种反馈方式。
 *
 * 不接收页面状态，仅接收所需的变量与回调。
 */
@Composable
fun ManualScreen(
    log: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "您可以通过以下方式将崩溃日志反馈给开发者，帮助我们修复问题：",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionItem(
                icon = Icons.Default.ContentCopy,
                title = "复制日志",
                description = "复制崩溃日志，并加入 QQ 群反馈给开发者",
                onClick = {
                    scope.launch {
                        clipboard.setText(log)
                        uriHandler.openUri(BuildConfig.MESSAGE_QQ_GROUP_URL)
                    }
                },
            )
            ActionItem(
                icon = Icons.Default.Email,
                title = "邮件发送",
                description = "通过邮件将崩溃日志发送给开发者",
                onClick = {
                    uriHandler.openUri(
                        "mailto:${BuildConfig.MESSAGE_MAIL}" +
                            "?subject=${"SYLU-EOA 崩溃日志反馈".urlEncode()}" +
                            "&body=${log.urlEncode()}"
                    )
                },
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun String.urlEncode(): String = buildString {
    for (byte in this@urlEncode.encodeToByteArray()) {
        val c = byte.toInt().and(0xFF).toChar()
        if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~') {
            append(c)
        } else {
            append('%')
            append(byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0'))
        }
    }
}
