package top.kagg886.report

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
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
        val model = viewModel { AppModel() }
        val state by model.collectAsState()

        GuideScaffold(
            modifier = Modifier.fillMaxSize(),
            title = {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = DefaultTransform,
                ) { s ->
                    Text(
                        when (s) {
                            AppModelState.Initializing -> "正在处理崩溃信息"
                            AppModelState.CrashManually -> "应用遇到了一个错误"
                            is AppModelState.CrashAutoUpload -> "正在上报崩溃日志"
                        }
                    )
                }
            },
            subTitle = {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = DefaultTransform,
                ) { s ->
                    Text(
                        when (s) {
                            AppModelState.Initializing -> "请稍候"
                            AppModelState.CrashManually -> "非常抱歉"
                            is AppModelState.CrashAutoUpload -> "自动处理中"
                        }
                    )
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
                    AnimatedContent(targetState = state, transitionSpec = DefaultTransform) { s ->
                        when (s) {
                            AppModelState.Initializing -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.sharedElement(sharedContentState = rememberSharedContentState("progress"), animatedVisibilityScope = this@AnimatedContent))
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
