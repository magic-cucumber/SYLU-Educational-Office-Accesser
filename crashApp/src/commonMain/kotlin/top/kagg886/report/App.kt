package top.kagg886.report

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.backend.database.AppDatabase
import top.kagg886.report.component.GuideScaffold
import top.kagg886.report.screen.AutoUploadScreen
import top.kagg886.report.screen.ManualScreen
import kotlin.random.Random

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/11 16:09
 * ================================================
 */

@Composable
fun CrashApp(database: AppDatabase, error: String, onRestart: () -> Unit) {
    val random = remember {
        Random(error.hashCode()).nextInt(36000) / 100.0f
    }

    val color =
        when ((AppSettingsMMKV.theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme()) || AppSettingsMMKV.theme == AppSettingsMMKVType.AppTheme.Dark) {
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
        val model = viewModel { AppModel(database, error) }
        val state by model.collectAsState()

        GuideScaffold(
            modifier = Modifier.fillMaxSize(),
            title = {
                val text = when (val state = state) {
                    AppModelState.Initializing -> "应用异常退出"

                    AppModelState.CrashManually -> "应用无法继续运行"

                    is AppModelState.CrashAutoUpload -> {
                        val success by state.success.collectAsState()
                        if (success) "我们已知悉本次崩溃" else "正在收集相关信息"
                    }
                }

                AnimatedContent(targetState = text, transitionSpec = DefaultTransform) { text ->
                    Text(text)
                }
            },
            subTitle = {
                val text = when (val s = state) {
                    AppModelState.Initializing ->
                        "正在准备处理本次异常，请稍候。"

                    AppModelState.CrashManually ->
                        "我们遇到了无法恢复的问题，需要重新启动才能继续使用。"

                    is AppModelState.CrashAutoUpload -> {
                        val success by s.success.collectAsState()
                        if (success)
                            "bug即将修复，请关注App后续更新。"
                        else
                            "正在整理本次问题，以帮助我们排查并改进应用。"

                    }
                }
                AnimatedContent(targetState = text, transitionSpec = DefaultTransform) { text ->
                    Text(text)
                }
            },
            confirmButton = {
                val restartEnabled = when (val s = state) {
                    AppModelState.Initializing -> false
                    AppModelState.CrashManually -> true
                    is AppModelState.CrashAutoUpload -> {
                        val success by s.success.collectAsState()
                        success
                    }
                }
                Button(
                    enabled = restartEnabled,
                    onClick = onRestart,
                ) {
                    Text("重新启动")
                }
            },
            content = {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            (fadeIn() togetherWith fadeOut()).using(SizeTransform(false))
                        }
                    ) { s ->
                        when (s) {
                            AppModelState.Initializing -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("progress"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                                )
                            }

                            AppModelState.CrashManually -> ManualScreen(log = error)

                            is AppModelState.CrashAutoUpload -> {
                                val progress by s.progress.collectAsState()
                                val label by s.label.collectAsState()
                                val success by s.success.collectAsState()
                                this@SharedTransitionLayout.AutoUploadScreen(
                                    progress = progress,
                                    label = label,
                                    success = success,
                                    scope = this
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}
