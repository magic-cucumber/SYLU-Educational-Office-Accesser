package top.kagg886.eoa.pages.main.settings.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.logcat.LogcatRoute
import top.kagg886.eoa.pages.main.MainScreen

@Serializable
data object AdvancedSettingsRoute

@Composable
fun AdvancedSettingsScreen() = MainScreen {
    val nav = LocalNavController.current
    val scope = rememberCoroutineScope()

    AdvancedSettingsScreenContent(
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
