package top.kagg886.eoa.pages.main.logcat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import co.touchlab.kermit.Severity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.collectAsLazyPagingItems

@Serializable
data object LogcatRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen() {
    val model = rootViewModel()
    var level by remember {
        mutableStateOf<Severity?>(null)
    }
    val scope = rememberCoroutineScope()
    val data = remember(level) {
        Pager(config = PagingConfig(10)) {
            model.appLogDao.all(level)
        }.flow.cachedIn(scope)
    }.collectAsLazyPagingItems()

    when {
        !data.loadState.isIdle && data.itemCount == 0 -> {
            // 显示加载指示器
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            val scrollState = rememberLazyListState()
            Column {
                TopAppBar(
                    title = {
                        Text("日志列表")
                    },
                    navigationIcon = {
                        BackIconButton()
                    },
                    actions = {
                        var show by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = {
                                show = true
                            }
                        ) {
                            Icon(Icons.Default.Menu,"")
                        }

                        DropdownMenu(
                            expanded = show,
                            onDismissRequest = {
                                show = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("全部")
                                },
                                leadingIcon = {
                                    if (level == null) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Red
                                        )
                                    }
                                },
                                onClick = { level = null }
                            )
                            Severity.entries.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(it.name)
                                    },
                                    leadingIcon = {
                                        if (it == level) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Red
                                            )
                                        }
                                    },
                                    onClick = { level = it }
                                )
                            }
                        }
                    }
                )

                if (data.itemCount == 0 && data.loadState.isIdle) {
                    ErrorPage(
                        title = { Text("警告") },
                        message = { Text("暂无日志") },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    return
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
                        Severity.Verbose -> Color.Gray
                        Severity.Debug -> Color.Blue
                        Severity.Info -> Color.Green
                        Severity.Warn -> Color(0xFFFF9800) // Orange
                        Severity.Error -> Color.Red
                        Severity.Assert -> Color(0xFF9C27B0) // Purple
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
                    text = formatTime(log.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // 日志消息
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium
            )
            // 如果有堆栈跟踪，显示它
            log.stacktrace?.let { stacktrace ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stacktrace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${
        localDateTime.minute.toString().padStart(2, '0')
    }:${localDateTime.second.toString().padStart(2, '0')}"
}