package top.kagg886.eoa.pages.logcat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.ExpandableText
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.collectAsLazyPagingItems
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.ChinaTimeFormater

@Serializable
data object LogcatRoute

@Composable
fun LogcatScreen() {
    val rootViewModel = rootViewModel()

    val model = viewModel {
        LogcatModel(rootViewModel.appLogDao)
    }

    val snack = LocalSnackBarHost.current
    model.collectSideEffect {
        when (it) {
            is LogcatSideEffect.ShowToast -> snack.showSnackBar(it.level, it.message)
        }
    }

    val state by model.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LogcatScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onTestButtonClicked = model::test,
            onExportButtonClicked = model::export,
            onClearButtonClicked = model::clean
        )

        var expand by remember {
            mutableStateOf(false)
        }

        LogcatScreenFAB(
            modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd),
            expand = expand,
            onExpandChangeRequest = { expand = it },
            severity = (state as? LogcatState.LoadingSuccess)?.severity,
            onSeverityChange = model::all
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogcatScreenContent(
    modifier: Modifier = Modifier,
    state: LogcatState,
    onTestButtonClicked: () -> Unit = {},
    onExportButtonClicked: () -> Unit = {},
    onClearButtonClicked: () -> Unit = {},
) = when (state) {
    LogcatState.Loading -> {}
    is LogcatState.LoadingSuccess -> {
        val data = state.flow.collectAsLazyPagingItems()

        when {
            !data.loadState.isIdle && data.itemCount == 0 -> {
                // 显示加载指示器
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val scrollState = rememberLazyListState()

                val isAtBottom by remember {
                    derivedStateOf {
                        scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
                    }
                }

                LaunchedEffect(data) {
                    var previousCount = data.itemCount
                    snapshotFlow { data.itemCount to isAtBottom }
                        .distinctUntilChanged()
                        .collect { (count, atBottom) ->
                            if (atBottom && !scrollState.isScrollInProgress && count > previousCount) {
                                scrollState.animateScrollToItem(0)
                            }
                            previousCount = count
                        }
                }

                Column(modifier) {
                    TopAppBar(
                        title = {
                            Text("日志列表")
                        },
                        navigationIcon = {
                            BackIconButton()
                        },
                        actions = {
                            var show by remember {
                                mutableStateOf(false)
                            }
                            IconButton(
                                onClick = {
                                    show = !show
                                }
                            ) {
                                AnimatedContent(
                                    targetState = show,
                                    label = "logcat-actions",
                                    transitionSpec = createMenuButtonAnim { show }
                                ) {
                                    Icon(
                                        imageVector = if (it) Icons.Default.Close else Icons.Default.Menu,
                                        contentDescription = null
                                    )
                                }
                            }
                            LogcatScreenActions(
                                show = show,
                                onTestButtonClicked = onTestButtonClicked,
                                onDismiss = { show = false },
                                onExportButtonClicked = onExportButtonClicked,
                                onClearButtonClicked = onClearButtonClicked
                            )
                        }
                    )

                    if (data.itemCount == 0 && data.loadState.isIdle) {
                        ErrorPage(
                            title = { Text("警告") },
                            message = { Text("暂无日志") },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                        return@Column
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        reverseLayout = true,
                        state = scrollState,
                    ) {
                        items(
                            count = data.itemCount,
                            key = { i -> data.peek(i)?.id ?: i },
                        ) { i ->
                            val item = data[i]
                            if (item != null) {
                                LogItem(log = item)
                            }
                        }

                        item(key = "Footer") {
                            if (!data.loadState.isIdle) {
                                // 底部加载指示器
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Text(
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    text = "没有更多数据了",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: AppLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日志级别标签
                Badge(
                    containerColor = when (log.level) {
                        Verbose -> Color.Gray
                        Debug -> Color.Blue
                        Info -> Color.Green
                        Warn -> Color(0xFFFF9800) // Orange
                        Error -> Color.Red
                        Assert -> Color(0xFF9C27B0) // Purple
                    }
                ) {
                    Text(
                        text = log.level.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Tag
                Text(
                    text = log.tag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 时间戳
                Text(
                    text = log.time.format(ChinaTimeFormater),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            var expand by remember {
                mutableStateOf(false)
            }
            // 日志消息
            ExpandableText(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                isExpanded = expand,
                onExpandChange = { expand = it }
            )
            // 如果有堆栈跟踪，显示它
            log.stacktrace?.let { stacktrace ->
                Spacer(modifier = Modifier.height(4.dp))
                ExpandableText(
                    text = stacktrace,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.error,
                    isExpanded = expand,
                    onExpandChange = { expand = it }
                )
            }
        }
    }
}

@Composable
private fun LogcatScreenActions(
    modifier: Modifier = Modifier,
    show: Boolean,
    onTestButtonClicked: () -> Unit,
    onDismiss: () -> Unit,
    onExportButtonClicked: () -> Unit,
    onClearButtonClicked: () -> Unit
) {
    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = {
                Text("测试日志")
            },
            leadingIcon = {
                Icon(Icons.Default.Kitesurfing, "test")
            },
            onClick = {
                onTestButtonClicked()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = {
                Text("导出日志")
            },
            leadingIcon = {
                Icon(Icons.Default.Share, "export")
            },
            onClick = {
                onExportButtonClicked()
                onDismiss()
            }
        )

        DropdownMenuItem(
            text = {
                Text("清空日志")
            },
            leadingIcon = {
                Icon(Icons.Default.Close, "close")
            },
            onClick = {
                onClearButtonClicked()
                onDismiss()
            }
        )
    }
}

@Composable
private fun LogcatScreenFAB(
    modifier: Modifier = Modifier,
    expand: Boolean,
    onExpandChangeRequest: (Boolean) -> Unit,

    severity: Severity?,
    onSeverityChange: (Severity?) -> Unit,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    AnimatedVisibility(
        visible = expand,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Column {
            SmallFloatingActionButton(
                onClick = {
                    onSeverityChange(null)
                },
            ) {
                Icon(Icons.Default.Clear, "clear")
            }
            Spacer(modifier = Modifier.height(4.dp))

            for (i in entries) {
                SmallFloatingActionButton(
                    onClick = {
                        onSeverityChange(i)
                    }
                ) {
                    Icon(
                        imageVector = when (i) {
                            Verbose -> Icons.Default.AllOut
                            Debug -> Icons.Default.BugReport
                            Info -> Icons.Default.Info
                            Warn -> Icons.Default.Warning
                            Error -> Icons.Default.Error
                            Assert -> Icons.Default.Bolt
                        },
                        contentDescription = null,
                        tint = if (i == severity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
    FloatingActionButton(
        onClick = { onExpandChangeRequest(!expand) },
        modifier = Modifier.size(60.dp)
    ) {
        Icon(Icons.Default.Menu, "menu")
    }
}
