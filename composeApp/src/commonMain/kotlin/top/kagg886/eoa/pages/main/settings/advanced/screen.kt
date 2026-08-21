package top.kagg886.eoa.pages.main.settings.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.logcat.LogcatRoute
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import kotlin.math.roundToInt

@Serializable
data object AdvancedSettingsRoute

@Composable
fun AdvancedSettingsScreen() = MainScreen {
    val nav = LocalNavController.current
    val scope = rememberCoroutineScope()

    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val animationSpeed by rootState.animationSpeed.collectAsState()

    AdvancedSettingsScreenContent(
        animationSpeed = animationSpeed,
        onAnimationSpeedChanged = {
            rootModel.postAnimationSpeed(it)
        },
        onLogcatClicked = {
            nav.navigate(LogcatRoute)
        },
        onMainThreadExceptionClicked = {
            throw IllegalStateException("高级设置手动触发主线程异常")
        },
        onCoroutineExceptionClicked = {
            scope.launch {
                throw IllegalStateException("高级设置手动触发协程异常")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSettingsScreenContent(
    animationSpeed: Float,
    onAnimationSpeedChanged: (Float) -> Unit,
    onLogcatClicked: () -> Unit,
    onMainThreadExceptionClicked: () -> Unit,
    onCoroutineExceptionClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("高级") },
                navigationIcon = { BackIconButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var animationSpeedDialog by remember { mutableStateOf(false) }
            if (animationSpeedDialog) {
                AlertDialog(
                    onDismissRequest = { animationSpeedDialog = false; },
                    confirmButton = {
                        TextButton(
                            onClick = { animationSpeedDialog = false; }
                        ) {
                            Text("确定")
                        }
                    },
                    title = { Text("当前速率: ${animationSpeed.displaySpeed()}") },
                    text = {
                        Column {
                            Slider(
                                value = animationSpeed,
                                valueRange = 0.1f..1.2f,
                                steps = 10,
                                onValueChange = { onAnimationSpeedChanged(it) },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("0.1x")
                                Text("1x")
                            }
                        }
                    }
                )
            }

            ListItem(
                headlineContent = { Text("全局动画速率") },
                supportingContent = { Text("开发用，如非必要请勿更改。") },
                leadingContent = {
                    Icon(
                        Icons.Default.Animation,
                        contentDescription = "动画速率",
                    )
                },
                modifier = Modifier.clickable { animationSpeedDialog = true }
            )

            ListItem(
                headlineContent = { Text("系统日志") },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.Notes,
                        contentDescription = "系统日志",
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "进入",
                    )
                },
                modifier = Modifier.clickable(onClick = onLogcatClicked)
            )

            ListItem(
                headlineContent = { Text("主线程抛出异常") },
                supportingContent = { Text("没事别点") },
                leadingContent = {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "主线程抛出异常",
                    )
                },
                modifier = Modifier.clickable(onClick = onMainThreadExceptionClicked)
            )

            ListItem(
                headlineContent = { Text("协程抛出异常") },
                supportingContent = { Text("没事别点") },
                leadingContent = {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "协程抛出异常",
                    )
                },
                modifier = Modifier.clickable(onClick = onCoroutineExceptionClicked)
            )
        }
    }
}

private fun Float.displaySpeed(): String =
    (this * 10).roundToInt().let { value ->
        if (value == 10) "1x" else "${value / 10}.${value % 10}x"
    }
