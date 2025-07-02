package top.kagg886.eoa.pages.main.home.notice

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.SystemNoticeEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.ExpandableText
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.ChinaDateFormater

@Serializable
data object SystemNoticeRoute

@Composable
fun SystemNoticeScreen() {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<SystemNoticeModel>(key = syncState.toString()) {
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
        onMarkAsRead = { noticeId -> model.markAsRead(noticeId) },
        onToggleIncludeAllClicked = { model.toggleIncludeAll() },
        onDismissed = { nav.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemNoticeContent(
    state: SystemNoticeState,
    onMarkAsRead: (SystemNoticeEntity) -> Unit,
    onDismissed: () -> Unit,
    onToggleIncludeAllClicked: () -> Unit
) {
    DialogPageScaffold(
        title = { Text(text = "系统通知") },
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        confirmButton = {
            TextButton(
                onClick = onToggleIncludeAllClicked,
            ) {
                Text(if (state is SystemNoticeState.HaveIncludeAllSettings && state.includeAll) "仅未读" else "全部")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissed,
            ) {
                Text(text = "返回")
            }
        },
    ) {
        when (state) {
            is SystemNoticeState.Failed -> {
                ErrorPage(
                    title = {
                        Text("数据同步失败")
                    },
                    message = {
                        Text(state.msg)
                    },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
                )
            }

            is SystemNoticeState.FailedButSuccess -> {
                ErrorPage(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
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
                        .fillMaxHeight(0.8f),
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

@Composable
private fun NoticeItem(
    notice: SystemNoticeEntity?,
    onMarkAsRead: () -> Unit
) {
    val showPlaceHolder by remember(notice) {
        derivedStateOf { notice == null }
    }
//
//    var isExpanded by remember { mutableStateOf(false) }
//    var hasOverflow by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
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

                    var isExpanded by remember {
                        mutableStateOf(false)
                    }
                    ExpandableText(
                        modifier = Modifier.placeholder(
                            visible = showPlaceHolder,
                            highlight = PlaceholderHighlight.shimmer()
                        ),
                        text = notice?.content ?: "正在加载通知内容...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        isExpanded = isExpanded,
                        onExpandChange = { isExpanded = it }
                    )

//                    Text(
//                        text = notice?.content ?: "正在加载通知内容...",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
//                        overflow = TextOverflow.Ellipsis,
//                        onTextLayout = { textLayoutResult: TextLayoutResult ->
//                            if (!showPlaceHolder) {
//                                val currentHasOverflow = textLayoutResult.hasVisualOverflow
//                                // 只有在收起状态下才更新hasOverflow，这样展开后仍然记住原本有溢出
//                                if (!isExpanded) {
//                                    hasOverflow = currentHasOverflow
//                                }
//                            }
//                        },
//                        modifier = Modifier
//                            .placeholder(
//                                visible = showPlaceHolder,
//                                highlight = PlaceholderHighlight.shimmer()
//                            )
//                            .animateContentSize(
//                                animationSpec = spring(
//                                    dampingRatio = Spring.DampingRatioNoBouncy,
//                                    stiffness = Spring.StiffnessMedium
//                                )
//                            )
//                    )

                    // 展开/收起按钮
//                    if (!showPlaceHolder && notice?.content?.isNotEmpty() == true && hasOverflow) {
//                        TextButton(
//                            onClick = { isExpanded = !isExpanded },
//                            modifier = Modifier.padding(top = 4.dp)
//                        ) {
//                            Icon(
//                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                                contentDescription = if (isExpanded) "收起" else "展开",
//                                modifier = Modifier.size(16.dp)
//                            )
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Text(
//                                text = if (isExpanded) "收起" else "展开",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }

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

                        Text(
                            text = notice?.time?.format(ChinaDateFormater) ?: "2024-01-01 00:00",
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
