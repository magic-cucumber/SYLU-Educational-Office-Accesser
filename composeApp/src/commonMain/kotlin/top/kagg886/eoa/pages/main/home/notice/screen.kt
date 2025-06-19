package top.kagg886.eoa.pages.main.home.notice

import StackedSnackbarHost
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import rememberStackedSnackbarHostState
import top.kagg886.backend.database.dao.SystemNoticeEntity
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object SystemNoticeRoute

@Composable
fun SystemNoticeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val snack = rememberStackedSnackbarHostState(maxStack = 3, StackedSnackbarAnimation.Slide)

        CompositionLocalProvider(
            LocalSnackBarHost provides snack
        ) {
            Surface(Modifier.fillMaxSize(0.8f), color = MaterialTheme.colorScheme.surface) {
                val mainViewModel = mainViewModel()
                val syncState by mainViewModel.collectAsState()
                val model = viewModel<SystemNoticeModel>(key = syncState.toString()) {
                    SystemNoticeModel(syncState, mainViewModel.database)
                }

                val snack = LocalSnackBarHost.current
                model.collectSideEffect {
                    when (it) {
                        is SystemNoticeSideEffect.Toast -> {
                            snack.showSnackBar(
                                type = it.type,
                                title = when (it.type) {
                                    SnackBarType.Success -> "成功"
                                    SnackBarType.Warning -> "警告"
                                    SnackBarType.Error -> "错误"
                                    SnackBarType.Info -> "信息"
                                },
                                description = it.message
                            )
                        }

                        SystemNoticeSideEffect.NavigateToLogin -> {

                        }
                    }
                }

                val state by model.collectAsState()
//
                SystemNoticeContent(
                    state = state,
                    onMarkAsRead = { noticeId -> model.markAsRead(noticeId) },
                    onToggleIncludeAllClicked = { model.toggleIncludeAll() }
                )
            }

            StackedSnackbarHost(
                modifier = Modifier.align(Alignment.BottomCenter),
                hostState = snack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemNoticeContent(
    state: SystemNoticeState,
    onMarkAsRead: (SystemNoticeEntity) -> Unit,
    onToggleIncludeAllClicked: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "系统通知",
                    )
                },
                navigationIcon = {
                    BackIconButton()
                },
                actions = {
                    var showDropdownMenu by remember {
                        mutableStateOf(false)
                    }

                    IconButton(
                        onClick = {
                            showDropdownMenu = true
                        },
                        enabled = state is SystemNoticeState.HaveIncludeAllSettings
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = {
                            showDropdownMenu = false
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(if (state is SystemNoticeState.HaveIncludeAllSettings && state.includeAll) "仅未读" else "全部")
                            },
                            onClick = {
                                showDropdownMenu = false
                                onToggleIncludeAllClicked()
                            },
                        )
                    }
                },
            )

            when (state) {
                is SystemNoticeState.Failed -> {
                    ErrorPage(
                        title = {
                            Text("数据同步失败")
                        },
                        message = {
                            Text(state.msg)
                        },
                        modifier = Modifier.fillMaxWidth()
                            .height(this@BoxWithConstraints.maxHeight - TopAppBarDefaults.MediumAppBarCollapsedHeight),
                    )
                }

                is SystemNoticeState.FailedButSuccess -> {
                    ErrorPage(
                        modifier = Modifier.fillMaxWidth()
                            .height(this@BoxWithConstraints.maxHeight - TopAppBarDefaults.MediumAppBarCollapsedHeight),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = {
                            Text(
                                text = "提示",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        message = {
                            Text(
                                text = state.msg,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    )

                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(this@BoxWithConstraints.maxHeight - TopAppBarDefaults.MediumAppBarCollapsedHeight),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val items = when (state) {
                            is SystemNoticeState.Success -> state.notices
                            else -> List(3) { null }
                        }

                        items(items) { notice ->
                            NoticeItem(notice) {
                                onMarkAsRead(notice!!)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeItem(
    notice: SystemNoticeEntity?,
    onMarkAsRead: () -> Unit
) {
    val showPlaceHolder by remember(notice) {
        derivedStateOf { notice == null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notice",
                            tint = if (notice?.isRead == false) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .placeholder(
                                    visible = showPlaceHolder,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = notice?.title ?: "加载中...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notice?.isRead == false) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = notice?.content ?: "正在加载通知内容...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.placeholder(
                            visible = showPlaceHolder,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(16.dp)
                                .placeholder(
                                    visible = showPlaceHolder,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        val timeFormat = remember {
                            kotlinx.datetime.LocalDateTime.Format {
                                year()
                                char('-')
                                monthNumber()
                                char('-')
                                dayOfMonth()
                                char(' ')
                                hour()
                                char(':')
                                minute()
                            }
                        }

                        Text(
                            text = notice?.time?.format(timeFormat) ?: "2024-01-01 00:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                    }
                }

                IconButton(
                    onClick = { onMarkAsRead() },
                    modifier = Modifier.size(32.dp).placeholder(
                        visible = showPlaceHolder,
                        highlight = PlaceholderHighlight.shimmer()
                    ),
                    enabled = notice?.isRead == false,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark as read",
                        tint = if (notice?.isRead != true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}