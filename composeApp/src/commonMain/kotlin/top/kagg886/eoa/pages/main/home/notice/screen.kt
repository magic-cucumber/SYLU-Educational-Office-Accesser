package top.kagg886.eoa.pages.main.home.notice

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.datetime.format
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.SystemNoticeEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.ExpandableText
import top.kagg886.eoa.component.bottomsheet.BottomSheetPageScaffold
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.ChinaDateFormater

@Serializable
data object SystemNoticeRoute

@Composable
fun SystemNoticeScreen() {
    val mainViewModel = mainViewModelOrNull() ?: return
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<SystemNoticeModel>(key = syncState.toViewModelKey()) {
        SystemNoticeModel(syncState, mainViewModel.database)
    }
    val nav = LocalNavController.current

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
                nav.navigate(LoginRoute) {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    val state by model.collectAsState()

    SystemNoticeContent(
        state = state,
        onMarkAsRead = { notice -> model.markAsRead(notice) },
        onExpandChange = { data, result -> model.toggleExpand(data, result) },
        onToggleIncludeAllClicked = { model.toggleIncludeAll() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemNoticeContent(
    state: SystemNoticeState,
    onMarkAsRead: (SystemNoticeEntity) -> Unit,
    onExpandChange: (SystemNoticeEntity, Boolean) -> Unit,
    onToggleIncludeAllClicked: () -> Unit
) {
    BottomSheetPageScaffold(maxExpandedHeight = LocalWindowInfo.current.containerDpSize.height * 0.8f) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .matchContent()
        ) {
            TopAppBar(
                windowInsets = WindowInsets(),
                title = { Text("系统通知") },
                navigationIcon = {
                    BackIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    )
                },
                actions = {
                    if (state is SystemNoticeState.HaveIncludeAllSettings) {
                        TextButton(onClick = onToggleIncludeAllClicked) {
                            Text(
                                text = if (state.includeAll) "仅未读" else "全部",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            when (state) {
                is SystemNoticeState.Failed -> {
                    ErrorPage(
                        title = { Text("数据同步失败") },
                        message = { Text(state.msg) },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }

                is SystemNoticeState.FailedButSuccess -> {
                    ErrorPage(
                        modifier = Modifier.fillMaxWidth().weight(1f),
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
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        val notices = when (state) {
                            is SystemNoticeState.Success -> state.notices
                            else -> List(3) { null }
                        }

                        items(notices) { notice ->
                            NoticeItem(
                                notice = notice,
                                isContentExpanded = (state as? SystemNoticeState.Success)?.expandableNotices?.contains(
                                    notice
                                ) ?: false,
                                onMarkAsRead = { notice?.let { onMarkAsRead(it) } },
                                onExpandChange = { result ->
                                    notice?.let {
                                        onExpandChange(
                                            it,
                                            result
                                        )
                                    }
                                },
                            )
                            if (notice !== notices.lastOrNull()) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeItem(
    notice: SystemNoticeEntity?,
    isContentExpanded: Boolean = false,
    onMarkAsRead: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
) {
    val showPlaceholder by remember(notice) {
        derivedStateOf { notice == null }
    }

    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = BottomSheetDefaults.ContainerColor
        ),
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showPlaceholder) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                } else {
                    BadgedBox(
                        badge = {
                            if (notice?.isRead == false) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "通知",
                            tint = if (notice?.isRead != false) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = notice?.title ?: "加载中...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (notice?.isRead == false) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.placeholder(
                    visible = showPlaceholder,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        },
        supportingContent = {
            Column {
                ExpandableText(
                    text = notice?.content ?: "正在加载通知内容...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.placeholder(
                        visible = showPlaceholder,
                        highlight = PlaceholderHighlight.shimmer()
                    ),
                    isExpanded = isContentExpanded,
                    onExpandChange = onExpandChange
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = notice?.time?.format(ChinaDateFormater) ?: "2024-01-01 00:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.placeholder(
                            visible = showPlaceholder,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }
        },
        trailingContent = {
            IconButton(
                onClick = onMarkAsRead,
                enabled = notice?.isRead == false,
                modifier = Modifier.placeholder(
                    visible = showPlaceholder,
                    highlight = PlaceholderHighlight.shimmer()
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "标记已读",
                    tint = if (notice?.isRead != true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}
