package top.kagg886.eoa.pages.main.home.gym.list

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.eoa.component.CollapseListItem
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.gym.GymDetail
import top.kagg886.eoa.gym.GymDetailHistory
import top.kagg886.eoa.gym.GymSummary
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.gym.GymScreen
import top.kagg886.eoa.pages.main.home.gym.GymState
import top.kagg886.eoa.pages.main.home.gym.gymViewModel
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.util.toFixed

@Composable
fun GymListScreen() = RevealContainer(3, AppInitializeMMKV::tutorialGymList) {
    GymScreen {
        val model = gymViewModel() ?: return@GymScreen
        val state by model.collectAsState()
        // 只有加载成功才展示顶栏刷新与筛选 FAB，加载中或失败时不展示。
        val showAction = state is GymState.Success
        val fabArrow = when (currentLayoutType()) {
            NavigationSuiteType.ShortNavigationBarCompact,
            NavigationSuiteType.ShortNavigationBarMedium,
            NavigationSuiteType.NavigationBar -> ContainerArrow.Top

            else -> ContainerArrow.Bottom
        }

        val homeFabModifier = Modifier.revealableAutoMeasured(2, fabArrow) {
            Text("点这里可以切换学年，查看历年的体测成绩。")
        }

        HomeScreen(
            route = EOAHomeModule.GYM,
            suite = {
                title { Text("体育测试") }
                menu {
                    val successState = state as? GymState.Success ?: return@menu

                    val refreshRotation = if (successState.loading) {
                        val refreshTransition = rememberInfiniteTransition(label = "gym-refresh")
                        val rotation by refreshTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart,
                            ),
                            label = "gym-refresh-rotation",
                        )
                        rotation
                    } else {
                        0f
                    }


                    IconButton(
                        onClick = { model.refresh() },
                        enabled = !successState.loading && successState.loginRequest == null,
                        modifier = Modifier.revealableAutoMeasured(0, ContainerArrow.Bottom) {
                            Text("点这里可以重新获取体测成绩。")
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier.rotate(refreshRotation),
                        )
                    }
                }
                if (showAction) {
                    fab(modifier = homeFabModifier) {
                        text { Text("筛选") }
                        icon { Icon(Icons.Default.FilterList, contentDescription = null) }
                        onClick { model.navigateToFilter() }
                    }
                }
            },
        ) {
            GymListScreenContent(
                state,
                onRetryLogin = { model.retryLogin() },
            )
        }
    }
}

@Composable
private fun GymListScreenContent(
    state: GymState,
    onRetryLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is GymState.Failed -> ErrorPage(
            title = { Text("体育测试加载失败") },
            message = { Text(state.message) },
            modifier = modifier.fillMaxSize()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = onRetryLogin
                ),
        )

        is GymState.Initial -> GymListContent(null, modifier)
        is GymState.Success -> GymListContent(state, modifier)
    }
}

@Composable
private fun GymListContent(state: GymState.Success?, modifier: Modifier = Modifier) {
    val entry = state?.value?.entries?.elementAtOrNull(state.currentSummaryIndex)
    if (state != null && entry == null) {
        ErrorPage(
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            },
            title = { Text("暂无体育测试数据") },
            message = { Text("轻点右上角刷新按钮重新获取") },
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    val lazyListState = remember(state?.lazyListState) { state?.lazyListState ?: LazyListState() }
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize().miuiLongShotSupport(lazyListState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            GymSummaryHeader(summary = entry?.key)
        }
        if (entry != null && entry.value.isEmpty()) {
            item { Text("暂无测试项目", modifier = Modifier.padding(vertical = 16.dp)) }
        }
        itemsIndexed(
            items = entry?.value ?: List(6) { null },
            key = { index, detail -> detail?.id ?: index },
        ) { index, detail ->
            GymDetailItem(
                detail = detail,
                history = if (entry != null && detail != null) {
                    state.histories[GymState.QueryKey(entry.key.id, detail.id)].orEmpty()
                } else emptyList(),
                modifier = Modifier.applyIf(index == 0) {
                    Modifier.revealableAutoMeasured(1, ContainerArrow.Top) {
                        Text("这里是体测成绩列表。点击某个项目可以查看详情和历年成绩。")
                    }
                }
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun GymSummaryHeader(summary: GymSummary?) {
    val loading = summary == null
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = summary?.asDisplayName() ?: "学年体育测试",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "已完成 ${summary?.done ?: 0}/${summary?.all ?: 0} 项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                )
            }
            Column(
                modifier = Modifier.padding(start = 16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "总分",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = summary?.score?.toFixed(2) ?: "00.00",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                )
            }
        }
    }
}

@Composable
private fun GymDetailItem(detail: GymDetail?, history: List<GymDetailHistory>,modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = detail == null || detail.isJoin == 1
    val disabledColor = ListItemDefaults.colors().disabledHeadlineColor
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else disabledColor
    val resultColor = if (enabled) MaterialTheme.colorScheme.primary else disabledColor
    val supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else disabledColor

    CollapseListItem(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        expanded = expanded,
        onExpandedChange = { if (detail != null) expanded = it },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (detail == null || !detail.grade.isNullOrBlank()) {
                    // 评价只是展示用的标签，用非交互的 Surface 实现，避免 AssistChip 消费点击导致父级无法展开。
                    Surface(
                        modifier = Modifier.width(48.dp).height(24.dp),
                        shape = AssistChipDefaults.shape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (enabled) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            disabledColor
                        },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = detail?.grade ?: "评价",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                    .placeholder(
                                        visible = detail == null,
                                        highlight = PlaceholderHighlight.shimmer(),
                                    ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Text(
                    text = detail?.name ?: "体育测试项目",
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).placeholder(
                        visible = detail == null,
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            detail == null -> "成绩：--"
                            detail.isJoin != 1 -> "未参加"
                            detail.result == null -> "暂无成绩"
                            else -> "${detail.result} ${detail.unit}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = resultColor,
                        maxLines = 1,
                        modifier = Modifier.placeholder(
                            visible = detail == null,
                            highlight = PlaceholderHighlight.shimmer(),
                        ),
                    )
                    Text(
                        text = "得分：${detail?.score?.toFixed(2) ?: "--"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = supportingColor,
                        maxLines = 1,
                        modifier = Modifier.placeholder(
                            visible = detail == null,
                            highlight = PlaceholderHighlight.shimmer(),
                        ),
                    )
                }
            }
        },
        support = {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                if (history.isEmpty()) {
                    Text(
                        "暂无历史记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                } else {
                    history.forEach { item ->
                        GymHistoryItem(
                            item = item,
                            unit = detail?.unit.orEmpty(),
                            enabled = enabled,
                        )
                    }
                }
            }
        },
    )
    HorizontalDivider()
}

@Composable
private fun GymHistoryItem(
    item: GymDetailHistory,
    unit: String,
    enabled: Boolean,
) {
    val disabledColor = ListItemDefaults.colors().disabledHeadlineColor
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else disabledColor
    val supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else disabledColor

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.scoreStatus == "2") {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已采用",
                tint = if (enabled) MaterialTheme.colorScheme.primary else disabledColor,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Spacer(Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "${item.result} $unit".trimEnd(),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.scoreTime.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                    .replace('T', ' '),
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
