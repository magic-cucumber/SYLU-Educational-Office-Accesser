package top.kagg886.eoa.pages.main.home.summary

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.database.dao.CourseExtendEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.notice.SystemNoticeModel
import top.kagg886.eoa.pages.main.home.notice.SystemNoticeRoute
import top.kagg886.eoa.pages.main.home.notice.SystemNoticeState
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.SettingsRoute
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.OverlayClip
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareBoundsComposed
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.util.toFixed

@Serializable
data object SummaryRoute

@Composable
fun SummaryScreen() = RevealContainer(3, AppInitializeMMKV::tutorialSummary) {
    val nav = LocalNavController.current

    val suiteArrow = when (currentLayoutType()) {
        NavigationSuiteType.NavigationBar -> ContainerArrow.Top
        else -> ContainerArrow.End
    }

    val fabArrow = when (currentLayoutType()) {
        NavigationSuiteType.NavigationBar -> ContainerArrow.Top
        else -> ContainerArrow.Bottom
    }
    HomeScreen(
        route = EOAHomeModule.SUMMARY,
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
        },


        suiteModifier = Modifier.revealableAutoMeasured(0, suiteArrow) {
            Text("这里是主导航。可以在概要、课表、考试等页面之间切换。")
        },
        menu = {
            val nav = LocalNavController.current
            IconButton(
                onClick = {
                    nav.navigate(SettingsRoute)
                },
                modifier = Modifier.revealableAutoMeasured(1, ContainerArrow.Bottom) {
                    Text("这里可以打开设置，修改账号、同步和显示相关选项。")
                },
            ) {
                Icon(Icons.Default.AccountBox, contentDescription = "返回")
            }
        },
        fabModifier = Modifier
            .revealableAutoMeasured(2, fabArrow) {
                Text("点这里查看学校通知，重要消息会集中放在这里。")
            }
            .composed {
                //借用 notice/screen
                val mainViewModel = mainViewModelOrNull() ?: return@composed Modifier
                val syncState by mainViewModel.collectAsState()
                val model = viewModel<SystemNoticeModel>(key = syncState.toViewModelKey()) {
                    SystemNoticeModel(syncState, mainViewModel.database)
                }
                val state by model.collectAsState()
                val color = MaterialTheme.colorScheme.error
                when(val state = state) {
                    is SystemNoticeState.Success -> {
                        val count = state.notices.count { !it.isRead }
                        if (count == 0) {
                            Modifier
                        } else {
                            val badgeText = count.toString()
                            val textMeasurer = rememberTextMeasurer()
                            val textStyle = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onError,
                            )
                            val textLayoutResult = remember(badgeText, textStyle) {
                                textMeasurer.measure(
                                    text = badgeText,
                                    style = textStyle,
                                )
                            }

                            Modifier.drawWithContent {
                                drawContent()
                                val badgeHeight = 16.dp.toPx()
                                val badgeHorizontalPadding = 4.dp.toPx()
                                val badgeWidth = maxOf(
                                    badgeHeight,
                                    textLayoutResult.size.width + badgeHorizontalPadding * 2,
                                )
                                val badgeLeft = size.width - badgeWidth

                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(badgeLeft, 0f),
                                    size = Size(badgeWidth, badgeHeight),
                                    cornerRadius = CornerRadius(badgeHeight / 2),
                                )
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(
                                        x = badgeLeft + (badgeWidth - textLayoutResult.size.width) / 2,
                                        y = (badgeHeight - textLayoutResult.size.height) / 2,
                                    ),
                                )
                            }
                        }
                    }
                    else -> return@composed Modifier
                }
            },
    ) {
        val mainViewModel = mainViewModelOrNull() ?: return@HomeScreen
        val syncState by mainViewModel.collectAsState()
        val rootModel = rootViewModel()
        val rootState by rootModel.collectAsState()
        val showExperimentClass by rootState.showExperimentClass.collectAsState()
        val model = viewModel<SummaryModel>(key = syncState.toViewModelKey()) {
            SummaryModel(syncState, mainViewModel.database)
        }
        model.collectSideEffect {
            when (it) {
                is SummarySideEffect.NavigateToCourseInfo -> {
                    nav.navigate(CourseDetailRoute(it.courseId))
                }

                is SummarySideEffect.NavigateToConflictInfo -> {
                    nav.navigate(CourseConflictRoute(it.weekNumber, it.dayOfWeek, it.periodOfDay))
                }
            }
        }
        val state by model.collectAsState()

        SummaryContentV2(
            state = state,
            showExperimentClass = showExperimentClass,
            onCourseItemClicked = { model.redirectToCourse(it) }
        )
    }
}

@Composable
private fun SummaryContentV2(
    state: SummaryState,
    showExperimentClass: Boolean,
    onCourseItemClicked: (TodayClass) -> Unit = {}
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val suiteType = currentLayoutType()
        val expandedContent = suiteType != NavigationSuiteType.NavigationBar && maxWidth >= 720.dp
        val horizontalContentPadding = if (expandedContent) 32.dp else 16.dp
        val contentPadding = if (expandedContent) {
            PaddingValues(
                start = horizontalContentPadding,
                top = 20.dp,
                end = horizontalContentPadding,
                bottom = 24.dp
            )
        } else {
            PaddingValues(
                start = horizontalContentPadding,
                top = 12.dp,
                end = horizontalContentPadding,
                bottom = 24.dp
            )
        }

        val lazyListState = rememberLazyListState()

        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize().miuiLongShotSupport(lazyListState)
        ) {
            item {
                TodayOverviewHeader(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (state) {
                is SummaryState.Failed -> item {
                    ErrorPage(
                        title = {
                            Text("数据同步失败")
                        },
                        message = {
                            Text(state.msg)
                        },
                        modifier = Modifier.fillMaxWidth().fillParentMaxHeight(0.72f),
                    )
                }

                is SummaryState.FailedButSuccess -> item {
                    ErrorPage(
                        modifier = Modifier.fillMaxWidth().fillParentMaxHeight(0.72f),
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

                is SummaryState.Loading -> {
                    items(3) {
                        CourseItem(
                            null,
                            onCourseItemClicked = onCourseItemClicked
                        )
                    }
                }

                is SummaryState.Success -> {
                    if (expandedContent) {
                        item {
                            ExpandedSummaryDashboard(
                                state = state,
                                showExperimentClass = showExperimentClass,
                                onCourseItemClicked = onCourseItemClicked,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        return@LazyColumn
                    }

                    if (showExperimentClass && state.extendClass.isNotEmpty()) {
                        item {
                            ExperimentSection(
                                extendClasses = state.extendClass,
                                compact = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (state.plan.isEmpty()) {
                        item {
                            TodayEmptyCard(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        items(state.plan) { course ->
                            CourseItem(
                                course = course,
                                onCourseItemClicked = onCourseItemClicked
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayOverviewHeader(
    state: SummaryState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        OverviewMetricRow(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OverviewMetricRow(
    state: SummaryState,
    modifier: Modifier = Modifier
) {
    val showPlaceHolder = state is SummaryState.Loading
    val week = when (state) {
        is SummaryState.Success -> "${state.weekNumber}"
        else -> "--"
    }
    val courses = when (state) {
        is SummaryState.Success -> "${state.plan.size}"
        else -> "--"
    }
    val progress = when (state) {
        is SummaryState.Success -> "${(state.progress * 100).toFixed(1)}%"
        else -> "--"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewMetric(
            title = "教学周",
            value = week,
            showPlaceHolder = showPlaceHolder,
            modifier = Modifier.weight(1f)
        )
        OverviewMetric(
            title = "今日课程",
            value = courses,
            showPlaceHolder = showPlaceHolder,
            modifier = Modifier.weight(1f)
        )
        OverviewMetric(
            title = "学期进度",
            value = progress,
            showPlaceHolder = showPlaceHolder,
            progressFraction = when (state) {
                is SummaryState.Success -> state.progress.coerceIn(0f, 1f)
                else -> null
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewMetric(
    title: String,
    value: String,
    showPlaceHolder: Boolean,
    progressFraction: Float? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val progressContentColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = modifier.heightIn(min = 72.dp),
        shape = shape,
        color = containerColor,
        contentColor = contentColor
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().clipToBounds()
        ) {
            val metricWidth = maxWidth

            OverviewMetricContent(
                title = title,
                value = value,
                showPlaceHolder = showPlaceHolder,
                modifier = Modifier.fillMaxWidth()
            )

            progressFraction?.let { fraction ->
                val progressWidth = metricWidth * fraction
                Box(
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight()
                            .width(progressWidth)
                            .background(progressColor)
                    )
                    CompositionLocalProvider(LocalContentColor provides progressContentColor) {
                        ClippedOverviewMetricContent(
                            title = title,
                            value = value,
                            showPlaceHolder = showPlaceHolder,
                            fullWidth = metricWidth,
                            clippedWidth = progressWidth,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClippedOverviewMetricContent(
    title: String,
    value: String,
    showPlaceHolder: Boolean,
    fullWidth: Dp,
    clippedWidth: Dp,
    modifier: Modifier = Modifier
) {
    Layout(
        content = {
            OverviewMetricContent(
                title = title,
                value = value,
                showPlaceHolder = showPlaceHolder
            )
        },
        modifier = modifier.width(clippedWidth).clipToBounds()
    ) { measurables, constraints ->
        val contentWidth = fullWidth.roundToPx()
        val visibleWidth = clippedWidth.roundToPx()
        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = contentWidth,
                maxWidth = contentWidth
            )
        )

        layout(
            width = visibleWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
            height = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            placeable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun OverviewMetricContent(
    title: String,
    value: String,
    showPlaceHolder: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.placeholder(
                visible = showPlaceHolder,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}

@Composable
private fun ExpandedSummaryDashboard(
    state: SummaryState.Success,
    showExperimentClass: Boolean,
    onCourseItemClicked: (TodayClass) -> Unit,
    modifier: Modifier = Modifier
) {
    val showExperimentSection = showExperimentClass && state.extendClass.isNotEmpty()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = if (showExperimentSection) Modifier.weight(1.65f) else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.plan.isEmpty()) {
                TodayEmptyCard(Modifier.fillMaxWidth())
            } else {
                state.plan.forEach { course ->
                    CourseItem(
                        course = course,
                        onCourseItemClicked = onCourseItemClicked
                    )
                }
            }
        }

        if (showExperimentSection) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExperimentSection(
                    extendClasses = state.extendClass,
                    compact = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TodayEmptyCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 160.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = "今天没有课程",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "今天没有课程",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "吃饭，睡觉，打游戏。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExperimentSection(
    extendClasses: List<CourseExtendEntity>,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (compact) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(extendClasses) { extendClass ->
                    ExperimentClassItem(
                        extendClass = extendClass,
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                extendClasses.forEach { extendClass ->
                    ExperimentClassItem(
                        extendClass = extendClass,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperimentClassItem(
    extendClass: CourseExtendEntity,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "实验课",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = extendClass.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = extendClass.teacherName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CourseItem(
    course: TodayClass?,
    modifier: Modifier = Modifier,
    onCourseItemClicked: (TodayClass) -> Unit,
) {
    val showPlaceHolder = course == null
    val startTime = course?.date?.first?.toString() ?: "08:00"
    val endTime = course?.date?.second?.toString() ?: "09:40"
    val courseName = when (course) {
        is TodayClass.Conflict -> "冲突课程 (${course.data.size}门)"
        is TodayClass.Single -> course.name
        null -> "课程名称"
    }
    val cardShape = RoundedCornerShape(8.dp)

    val cardModifier = modifier
        .fillMaxWidth()
        .applyIf(course is TodayClass.Single) {
            shareBoundsComposed(
                sharedContentState = rememberSharedContentState(
                    key = "summary-course-to-detail-${(course as TodayClass.Single).recordId}"
                ),
                animatedVisibilityScope = LocalAnimatedContentScope.current,
                resizeMode = RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(cardShape)
            )
        }
        .clip(cardShape)

    ElevatedCard(
        onClick = { course?.let { onCourseItemClicked(it) } },
        enabled = course != null,
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val progress = course?.progress?.collectAsState(null)?.value
            progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CourseTimeBlock(
                    startTime = startTime,
                    endTime = endTime,
                    showPlaceHolder = showPlaceHolder
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.placeholder(
                            visible = showPlaceHolder,
                            highlight = PlaceholderHighlight.shimmer()
                        ).applyIf(course is TodayClass.Single) {
                            shareElementComposed(
                                sharedContentState = rememberSharedContentState(
                                    key = "summary-course-name-to-detail-${(course as TodayClass.Single).recordId}"
                                ),
                                animatedVisibilityScope = LocalAnimatedContentScope.current
                            )
                        }
                    )

                    when (course) {
                        is TodayClass.Single -> {
                            InfoLine(
                                icon = Icons.Default.Place,
                                text = course.location,
                                contentDescription = "上课地点",
                                showPlaceHolder = showPlaceHolder
                            )
                            InfoLine(
                                icon = Icons.Default.Person,
                                text = course.teacher,
                                contentDescription = "授课教师",
                                showPlaceHolder = showPlaceHolder
                            )
                        }

                        is TodayClass.Conflict -> {

                            when (course.data.size) {
                                2 -> {
                                    for (i in course.data) {
                                        InfoLine(
                                            icon = Icons.Default.CalendarToday,
                                            text = i.name,
                                            contentDescription = "课程名称",
                                            showPlaceHolder = showPlaceHolder
                                        )
                                    }
                                }

                                else -> {
                                    InfoLine(
                                        icon = Icons.Default.CalendarToday,
                                        text = course.data.first().name,
                                        contentDescription = "课程名称",
                                        showPlaceHolder = showPlaceHolder
                                    )

                                    InfoLine(
                                        icon = Icons.AutoMirrored.Filled.More,
                                        text = "+ ${course.data.size - 2}", //正好为3个时，为 前面的2个 + 更多的占位符
                                        contentDescription = "更多",
                                        showPlaceHolder = showPlaceHolder
                                    )
                                }
                            }
                        }

                        null -> {
                            InfoLine(
                                icon = Icons.Default.Person,
                                text = "",
                                contentDescription = "授课教师",
                                showPlaceHolder = showPlaceHolder
                            )
                            InfoLine(
                                icon = Icons.Default.Person,
                                text = "",
                                contentDescription = "授课教师",
                                showPlaceHolder = showPlaceHolder
                            )
                        }
                    }

                    if (showPlaceHolder) {
                        Surface(
                            modifier = Modifier.size(width = 128.dp, height = 24.dp).placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer()
                            ),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            content = {}
                        )
                    } else {
                        CourseBadges(course)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseTimeBlock(
    startTime: String,
    endTime: String,
    showPlaceHolder: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(72.dp).heightIn(min = 72.dp).placeholder(
            visible = showPlaceHolder,
            highlight = PlaceholderHighlight.shimmer()
        ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = startTime,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f)
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InfoLine(
    icon: ImageVector,
    text: String,
    contentDescription: String,
    showPlaceHolder: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.placeholder(
                visible = showPlaceHolder,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}

@Composable
private fun CourseBadges(course: TodayClass?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (course?.progress != null) {
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

        if ((course as? TodayClass.Single)?.isDegreeProgram == true) {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Text(
                    text = "学位",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if ((course as? TodayClass.Single)?.isExamine == true) {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Text(
                    text = "考试",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
