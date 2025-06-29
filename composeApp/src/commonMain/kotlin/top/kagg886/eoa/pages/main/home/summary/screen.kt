package top.kagg886.eoa.pages.main.home.summary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.notice.SystemNoticeRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.util.toFixed

@Serializable
data object SummaryRoute

@Composable
fun SummaryScreen() {
    val nav = LocalNavController.current
    HomeScreen(
        route = NavigationRoute.SUMMARY,
        title = {
            Text("概要")
        },
        fabIcon = {
            Icon(Icons.Default.Mail, "mail")
        },
        fabText = {
            Text("通知")
        },
        fabOnClick = {
            nav.navigate(SystemNoticeRoute)
        }
    ) {
        val mainViewModel = mainViewModel()
        val syncState by mainViewModel.collectAsState()
        val model = viewModel<SummaryModel>(key = syncState.toString()) {
            SummaryModel(syncState, mainViewModel.database)
        }
        model.collectSideEffect {
            when (it) {
                is SummarySideEffect.NavigateToCourseInfo -> {
                    nav.navigate(CourseDetailRoute(it.courseId))
                }
            }
        }
        val state by model.collectAsState()

        SummaryContentV2(
            state = state,
            syncState = syncState,
            onCourseItemClicked = { model.redirectToCourse(it) },
            onSyncActionStarted = { mainViewModel.startSyncForce() }
        )
    }
}

@Composable
private inline fun SummaryContentV2(
    state: SummaryState,
    syncState: MainRouteViewState,
    noinline onCourseItemClicked: (TodayClass) -> Unit = {},
    noinline onSyncActionStarted: () -> Unit = {}
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        var cardHeight by remember {
            mutableStateOf(0.dp)
        }
        val density = LocalDensity.current
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.matchParentSize()
        ) {
            item {
                SummaryContentPlaceHolderHeader(
                    state = state,
                    modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                        cardHeight = with(density) { it.size.height.toDp() }
                    },
                    onSyncActionStarted = onSyncActionStarted,
                    syncState = syncState,
                    suiteType = currentLayoutType()
                )
            }

            when (state) {
                is SummaryState.Failed -> item {
                    // 加载失败的页面
                    ErrorPage(
                        title = {
                            Text("数据同步失败")
                        },
                        message = {
                            Text(state.msg)
                        },
                        modifier = Modifier.fillMaxWidth().height(maxHeight - cardHeight - 64.dp),
                    )
                }

                is SummaryState.FailedButSuccess -> item {
                    ErrorPage(
                        modifier = Modifier.fillMaxWidth().height(maxHeight - cardHeight - 64.dp),
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

                is SummaryState.Loading -> items(3) { _ ->
                    CourseItem(null, onCourseItemClicked)
                }

                is SummaryState.Success -> {
                    if (state.plan.isEmpty()) {
                        item {
                            ErrorPage(
                                modifier = Modifier.fillMaxWidth()
                                    .height(maxHeight - cardHeight - 64.dp),
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
                                        text = "今天没有课程",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )

                                }
                            )
                        }
                        return@LazyColumn
                    }
                    items(state.plan) { course ->
                        CourseItem(course, onCourseItemClicked)
                    }
                }
            }
        }
    }
}

@Composable
private inline fun SyncStateCard(
    state: MainRouteViewState,
    noinline onSyncActionStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "系统状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val (text, icon, containerColor, contentColor, enabled) = when (state) {
                is MainRouteViewState.Empty -> {
                    Tuple5(
                        "系统正在初始化",
                        null,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        false
                    )
                }

                is MainRouteViewState.SyncProcess -> {
                    Tuple5(
                        "系统正在同步",
                        Icons.Default.Refresh,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        false
                    )
                }

                is MainRouteViewState.SyncSuccess -> {
                    Tuple5(
                        "系统同步成功",
                        Icons.Default.Check,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        true
                    )
                }

                is MainRouteViewState.SyncFailed -> {
                    Tuple5(
                        "系统同步失败",
                        Icons.Default.Close,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.onError,
                        true
                    )
                }
            }

            val animatedContainerColor by animateColorAsState(
                targetValue = containerColor,
                animationSpec = tween(durationMillis = 300),
                label = "containerColorAnimation"
            )

            val animatedContentColor by animateColorAsState(
                targetValue = contentColor,
                animationSpec = tween(durationMillis = 300),
                label = "contentColorAnimation"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "infiniteRotation")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing)
                ),
                label = "rotationAnimation"
            )

            ExtendedFloatingActionButton(
                onClick = { if (enabled) onSyncActionStarted() },
                modifier = Modifier.fillMaxWidth(),
                containerColor = animatedContainerColor,
                contentColor = animatedContentColor
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = icon,
                        transitionSpec = createMenuButtonAnim { targetState != initialState }
                    ) {
                        if (it != null) {
                            Icon(
                                imageVector = it,
                                contentDescription = text,
                                modifier = Modifier
                                    .rotate(if (state is MainRouteViewState.SyncProcess) rotationAngle else 0f)
                                    .size(18.dp)
                            )
                            return@AnimatedContent
                        }
                        Box(Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = text,
                        transitionSpec = createMenuButtonAnim { targetState != initialState },
                        label = "textAnimation"
                    ) { animatedText ->
                        Text(
                            text = animatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Helper data class for tuple
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
private inline fun SummaryCard(
    state: SummaryState,
    modifier: Modifier = Modifier
) {
    val showPlaceHolder by remember(state) {
        derivedStateOf {
            state is SummaryState.Loading
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Current week indicator
                SummaryItem(
                    showPlaceHolder = showPlaceHolder,
                    details = when (state) {
                        is SummaryState.Success -> "第 ${state.weekNumber} 周"
                        is SummaryState.Failed -> "获取失败"
                        is SummaryState.Loading -> "加载中"
                        is SummaryState.FailedButSuccess -> state.msg
                    },
                    title = "当前周数",
                )
                SummaryItem(
                    showPlaceHolder = showPlaceHolder,
                    details = when (state) {
                        is SummaryState.Success -> "共 ${state.plan.size} 节课"
                        is SummaryState.Failed -> "获取失败"
                        is SummaryState.Loading -> "加载中"
                        is SummaryState.FailedButSuccess -> state.msg
                    },
                    title = "今日课程数",
                )

                SummaryItem(
                    showPlaceHolder = showPlaceHolder,
                    details = when (state) {
                        is SummaryState.Success -> "${(state.progress * 100).toFixed(2)}%"
                        is SummaryState.Failed -> "获取失败"
                        is SummaryState.Loading -> "加载中"
                        is SummaryState.FailedButSuccess -> state.msg
                    },
                    title = "学期进度"
                )

            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
private inline fun SummaryContentPlaceHolderHeader(
    modifier: Modifier = Modifier,
    suiteType: NavigationSuiteType,
    syncState: MainRouteViewState,
    state: SummaryState,
    noinline onSyncActionStarted: () -> Unit,
) {
    when (suiteType) {
        NavigationSuiteType.NavigationBar -> {
            Column(modifier) {
                SyncStateCard(
                    state = syncState,
                    onSyncActionStarted = onSyncActionStarted,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                SummaryCard(state, Modifier.fillMaxWidth())
            }
        }

        else -> {
            Row(
                modifier.height(IntrinsicSize.Min)
                    .applyIf(suiteType == NavigationSuiteType.NavigationDrawer) { padding(vertical = 16.dp) }) {
                SyncStateCard(
                    state = syncState,
                    onSyncActionStarted = onSyncActionStarted,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(16.dp))
                SummaryCard(state, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private inline fun CourseItem(
    course: TodayClass?,
    noinline onCourseItemClicked: (TodayClass) -> Unit
) {
    val showPlaceHolder by remember(course) {
        derivedStateOf {
            course == null
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shareElementComposed(
                sharedContentState = rememberSharedContentState(key = "summary-course-to-detail-${course?.recordId}"),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            )
            .clickable(onClick = { course?.let { onCourseItemClicked(it) } }),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Progress bar for ongoing classes
            course?.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = course?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )

                    // Show an indicator for ongoing classes
                    course?.progress?.let {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "进行中",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )

                    Text(
                        text = course?.date?.let { (start, end) -> "$start - $end" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )

                    Text(
                        text = course?.location ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "授课教师: ${course?.teacher ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.placeholder(
                        visible = showPlaceHolder,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            }
        }
    }
}

@Composable
private inline fun SummaryItem(
    showPlaceHolder: Boolean,
    details: String,
    title: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(width = 80.dp, height = 48.dp)
                .placeholder(
                    visible = showPlaceHolder,
                    highlight = PlaceholderHighlight.shimmer()
                ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.placeholder(
                visible = showPlaceHolder,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}
