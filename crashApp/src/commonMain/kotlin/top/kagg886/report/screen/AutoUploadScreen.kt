package top.kagg886.report.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 自动上报页：仅对外部状态做出反应，不包含任何交互类 UI。
 *
 * @param progress 上传进度，取值 [0, 1]；为 null 时表示进度未知（不确定进度）。
 * @param label 当前上传阶段的描述文本。
 * @param success 是否上传成功；成功时进度条替换为成功图标。
 */
@Composable
fun SharedTransitionScope.AutoUploadScreen(
    progress: Float?,
    label: String,
    success: Boolean,
    modifier: Modifier = Modifier,
    scope: AnimatedContentScope,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                success -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary,
                )

                progress == null -> CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize()
                        .sharedElement(rememberSharedContentState("progress"), scope),
                )

                else -> CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                        .sharedElement(rememberSharedContentState("progress"), scope),
                )
            }
        }

        Spacer(Modifier.height(24.dp))


        AnimatedContent(
            targetState = label,
            transitionSpec = {
                slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

