package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDateTime
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.course.ComponentDefault
import top.kagg886.eoa.component.course.CourseComponentHeader
import top.kagg886.eoa.component.course.CourseLayout
import top.kagg886.eoa.component.course.CourseTimelineScope
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.course.detail.sharedBoundsKey
import top.kagg886.eoa.pages.main.home.summary.TodayClass
import top.kagg886.eoa.pages.main.home.summary.hasCourseConflict
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareBoundsComposed
import top.kagg886.eoa.util.rememberVerticalZoomState
import top.kagg886.eoa.util.verticalZoom
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes

@Composable
fun CoursePageListScreen(
    index: Int,
    isCurrentPage: Boolean,
    scale: Float,
    onZoomChange: (Float) -> Unit,
) {
    val nav = LocalNavController.current

    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()

    val theme by rootState.theme.collectAsState()
    val systemNight = isSystemInDarkTheme()
    val useNightMode = remember(theme, systemNight) {
        when (theme) {
            AppSettingsMMKVType.AppTheme.Dark -> true
            AppSettingsMMKVType.AppTheme.Light -> false
            AppSettingsMMKVType.AppTheme.SystemDefault -> systemNight
        }
    }
    val hideWeekendCourse by rootState.hideWeekendCourse.collectAsState()

    val mainViewModel = mainViewModelOrNull() ?: return
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CoursePageViewModel>(
        key = "${index * 31 + syncState.toViewModelKey().hashCode()}"
    ) {
        CoursePageViewModel(
            syncState, index + 1, mainViewModel.database, rootState.showHolidayCourse
        )
    }
    val state by model.collectAsState()
    model.collectSideEffect {
        when (it) {
            is CoursePageSideEffect.NavigateToCourseDetail -> {
                nav.navigate(it.route)
            }

            is CoursePageSideEffect.NavigateToConflictDetail -> {
                nav.navigate(CourseConflictRoute(it.startTime, it.endTime))
            }
        }
    }

    CoursePageScreenContent(
        state = state,
        isCurrentPage = isCurrentPage,
        initialScale = scale,
        useNightMode = useNightMode,
        hideWeekendCourse = hideWeekendCourse,
        onCourseItemClicked = {
            model.navigateToCourseDetail(it)
        },
        onCourseConflictClicked = { startTime, endTime ->
            model.navigateToConflictDetail(startTime, endTime)
        },
        onZoomChange = onZoomChange
    )
}

@Composable
private fun CoursePageScreenContent(
    state: CoursePageState,
    isCurrentPage: Boolean,
    initialScale: Float,
    useNightMode: Boolean,
    hideWeekendCourse: Boolean,
    onCourseItemClicked: (TodayClass.Single) -> Unit,
    onCourseConflictClicked: (LocalDateTime, LocalDateTime) -> Unit,
    onZoomChange: (Float) -> Unit,
) {
    when (state) {
        is CoursePageState.Failed -> {
            ErrorPage(
                title = {
                    Text("加载课表失败")
                },
                message = {
                    Text(state.msg)
                }
            )
        }

        is CoursePageState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "正在加载课表，请稍等。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is CoursePageState.Success -> {
            CoursePageScreenSuccess(
                thisWeekStartDate = state.thisWeekStartDate,
                currentDate = state.currentDate,
                currentWeekCourse = state.currentWeekCourse,
                period = state.period,
                initialScale = initialScale,
                useNightMode = useNightMode,
                hideWeekendCourse = hideWeekendCourse,
                longShotEnabled = isCurrentPage,
                onCourseItemClicked = onCourseItemClicked,
                onCourseConflictClicked = onCourseConflictClicked,
                onZoomChange = onZoomChange,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CoursePageScreenSuccess(
    thisWeekStartDate: LocalDate,
    currentDate: LocalDate,
    currentWeekCourse: Map<Int, List<TodayClass>>,
    period: Map<Int, Pair<LocalTime, LocalTime>>,
    initialScale: Float,
    useNightMode: Boolean,
    hideWeekendCourse: Boolean,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    longShotEnabled: Boolean,
    onCourseItemClicked: (TodayClass.Single) -> Unit,
    onCourseConflictClicked: (LocalDateTime, LocalDateTime) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCourseKey by remember {
        mutableStateOf<CourseSegmentKey?>(null)
    }
    val expansionProgress = remember {
        Animatable(0f)
    }

    val zoomState = rememberVerticalZoomState(initialScale = initialScale)

    val coroutineScope = rememberCoroutineScope()
    val zoomEnabled = expandedCourseKey == null
    val density = LocalDensity.current

    val timelineDividerOffset = with(density) {
        (4.dp + MaterialTheme.typography.labelSmall.lineHeight.toDp()) / 2
    }
    val timelineScaleOriginPx = with(density) {
        (ComponentDefault.HeaderHeight + timelineDividerOffset).toPx()
    }

    fun expandCourse(course: TodayClass.Single) {
        expandedCourseKey = course.segmentKey
        coroutineScope.launch {
            expansionProgress.animateTo(
                targetValue = 1f, animationSpec = CourseExpansionAnimationSpec
            )
        }
    }

    fun dismissExpandedCourse() {
        val dismissingKey = expandedCourseKey ?: return
        coroutineScope.launch {
            expansionProgress.animateTo(
                targetValue = 0f, animationSpec = CourseExpansionAnimationSpec
            )
            if (expandedCourseKey == dismissingKey) {
                expandedCourseKey = null
            }
        }
    }

    fun handleCourseClicked(course: TodayClass) {
        val currentExpandedKey = expandedCourseKey

        if (currentExpandedKey != null) {
            if (course is TodayClass.Single && course.segmentKey == currentExpandedKey) {
                expandedCourseKey = null
                coroutineScope.launch {
                    expansionProgress.snapTo(0f)
                }
                onCourseItemClicked(course)
            } else {
                dismissExpandedCourse()
            }
            return
        }

        when (course) {
            is TodayClass.Single -> {
                if (course.hasCourseConflict) {
                    expandCourse(course)
                } else {
                    onCourseItemClicked(course)
                }
            }

            is TodayClass.Conflict -> {
                onCourseConflictClicked(
                    course.date.first, course.date.second
                )
            }
        }
    }

    LaunchedEffect(longShotEnabled) {
        if (!longShotEnabled) {
            expansionProgress.snapTo(0f)
            expandedCourseKey = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = expandedCourseKey != null,
                onClick = ::dismissExpandedCourse
            )

            .verticalZoom(
                state = zoomState,
                scrollState = scrollState,
                enabled = zoomEnabled,
                scaleOriginPx = timelineScaleOriginPx,
                onZoomChange = onZoomChange,
            )

            .miuiLongShotSupport(
                enabled = longShotEnabled, scrollState = scrollState
            )
    ) {
        val timeAxisWidth = when {
            maxWidth < 600.dp -> 48.dp
            maxWidth < 840.dp -> 56.dp
            else -> 64.dp
        }

        val visibleDays = remember(hideWeekendCourse, currentWeekCourse) {
            if (hideWeekendCourse && currentWeekCourse[6].isNullOrEmpty() && currentWeekCourse[7].isNullOrEmpty()) {
                1..5
            } else {
                1..7
            }
        }

        val timelineRange = remember(currentWeekCourse) { timelineRangeOf(currentWeekCourse) }

        val scale = zoomState.scale
        //网格线横跨所有日期列，只能画在 CourseLayout 上；刻度间隔在此算好，同时用于网格与 TimeAxis
        val tickIntervalMinutes = when {
            scale >= zoomState.maxScale -> 15
            scale >= 1f -> 30
            else -> 60
        }
        val timelineHeight = timelineRange.durationMinutes * 2.4.dp * scale
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val days = visibleDays.toList()

        Column {
            CourseComponentHeader(
                weekStartDate = thisWeekStartDate,
                currentDate = currentDate,
                allowIsoWeekNumber = days,
                timelineWidth = timeAxisWidth,
            )
            // Label centering belongs to this UI, not the reusable time coordinates.
            Box(Modifier.padding(top = timelineDividerOffset)) {
                CourseLayout(
                    startTime = timelineRange.start,
                    endTime = timelineRange.end,
                    allowIsoWeekNumber = days,
                    timelineWidth = timeAxisWidth,
                    modifier = Modifier.fillMaxWidth().height(timelineHeight).drawBehind {
                        for (minute in timelineRange.marks(tickIntervalMinutes)) {
                            val y = (minute - timelineRange.start.toSecondOfDay() / 60f) / timelineRange.durationMinutes * size.height
                            drawLine(
                                color = gridColor.copy(alpha = if (minute % MinutesPerHour == 0) 0.5f else 0.25f),
                                start = Offset(timeAxisWidth.toPx(), y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                    },
                ) {
                    timeline {
                        TimeAxis(period = period, tickIntervalMinutes = tickIntervalMinutes)
                    }
                    for (day in days) {
                        for (course in currentWeekCourse[day].orEmpty()) {
                            key(course.segmentKey) {
                                val isExpanded =
                                    course is TodayClass.Single && course.segmentKey == expandedCourseKey
                                card(
                                    isoWeekNumber = day,
                                    startTime = {
                                        val start = course.date.first.time
                                        if (isExpanded) {
                                            interpolateTime(
                                                start,
                                                course.fullDate.first.time,
                                                expansionProgress.value
                                            )
                                        } else start
                                    },
                                    endTime = {
                                        val end = course.date.second.time
                                        if (isExpanded) {
                                            interpolateTime(
                                                end,
                                                course.fullDate.second.time,
                                                expansionProgress.value
                                            )
                                        } else end
                                    },
                                    modifier = Modifier
                                        .zIndex(if (isExpanded) 99999f else 0f)
                                        .padding(horizontal = 3.dp, vertical = 1.dp),
                                ) {
                                    CourseCalendarCard(
                                        course = course,
                                        useNightMode = useNightMode,
                                        onClick = { handleCourseClicked(course) },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseTimelineScope.TimeAxis(
    period: Map<Int, Pair<LocalTime, LocalTime>>,
    tickIntervalMinutes: Int,
) {
    //period为空（未同步过节次信息）时固定为时钟刻度视图，且不允许切换
    var showPeriods by rememberSaveable { mutableStateOf(true) }
    val periodMode = showPeriods && period.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = period.isNotEmpty(),
            ) { showPeriods = !showPeriods },
    ) {
        AnimatedContent(
            targetState = periodMode,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(280)))
                    .togetherWith(slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(280)))
            },
            label = "TimeAxisMode",
        ) { isPeriodMode ->
            //内部Box填满整个轴列，与外层Box同边界，子组件的align/offset语义不变
            Box(Modifier.fillMaxSize()) {
                //嵌套lambda的receiver遮蔽了外层CourseTimelineScope，需显式取回
                with(this@TimeAxis) {
                    if (isPeriodMode) {
                        PeriodLabels(period)
                    } else {
                        ClockTickLabels(TimelineRange(startTime, endTime), tickIntervalMinutes)
                    }
                }
            }
        }
    }
}

/** 节次视图（默认）：每个节次一个块，显示节次号与起止时间。 */
@Composable
private fun CourseTimelineScope.PeriodLabels(
    period: Map<Int, Pair<LocalTime, LocalTime>>,
) {
    //沿用MonthHeader的filled tonal风格，略降alpha让层级低于列头
    val containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    for ((index, time) in period) {
        val (start, end) = time
        //只渲染完整落在可视区间内的节次；区间外的offsetOf会越界
        if (start < startTime || end > endTime) continue
        val top = offsetOf(start)
        val height = offsetOf(end) - top
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = top)
                .fillMaxWidth()
                .height(height)
                .background(containerColor, RoundedCornerShape(10.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
            )
            //缩放过矮时只显示节次号，避免时间文字溢出到相邻块
            if (height >= 56.dp) {
                Text(
                    text = start.toTimeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                )
                Text(
                    text = end.toTimeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** 时钟刻度视图：原 TimeAxis 逻辑。 */
@Composable
private fun CourseTimelineScope.ClockTickLabels(range: TimelineRange, tickIntervalMinutes: Int) {
    val halfLabelHeight = with(LocalDensity.current) {
        MaterialTheme.typography.labelSmall.lineHeight.toDp() / 2
    }
    for (minute in range.marks(tickIntervalMinutes)) {
        val isHourMark = minute % MinutesPerHour == 0
        Text(
            text = minute.toTimeLabel(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = offsetOf(LocalTime.fromSecondOfDay(minute * 60)) - halfLabelHeight),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHourMark) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isHourMark) 0.8f else 0.55f),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun timelineRangeOf(currentWeekCourse: Map<Int, List<TodayClass>>): TimelineRange {
    val courses = currentWeekCourse.values.flatten()
    val defaultStart = LocalTime(8, 0)
    val defaultEnd = LocalTime(22, 0)

    //保护课程为空的情况下使用minOf会crash的bug，这种情况为一周都没有课程
    if (courses.isEmpty()) {
        return TimelineRange(defaultStart, defaultEnd)
    }

    val earliest = courses.minOf { course ->
        when (course) {
            is TodayClass.Single -> minOf(
                course.date.first.time,
                course.fullDate.first.time,
            )

            else -> course.date.first.time
        }
    }

    val latest = courses.maxOf { course ->
        when (course) {
            is TodayClass.Single -> maxOf(
                course.date.second.time,
                course.fullDate.second.time,
            )

            else -> course.date.second.time
        }
    }

    //对最早课程和最晚课程取约束
    val start = minOf(defaultStart, earliest)
    val end = maxOf(defaultEnd, latest)

    return TimelineRange(
        start = start,
        end = end
    )
}

private fun interpolateTime(start: LocalTime, end: LocalTime, progress: Float): LocalTime {
    val from = start.toNanosecondOfDay()
    val to = end.toNanosecondOfDay()
    return LocalTime.fromNanosecondOfDay(from + ((to - from) * progress.toDouble()).roundToLong())
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CourseCalendarCard(
    course: TodayClass, useNightMode: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val single = course as? TodayClass.Single

    val route = single?.let {
        CourseDetailRoute(
            recordId = it.recordId,
            source = "list",
            startTime = it.date.first,
            endTime = it.date.second,
        )
    }

    val sharedBoundsKey = route?.sharedBoundsKey

    val colorSeed = single?.name.orEmpty().hashCode()

    val pastel = remember(
        colorSeed, useNightMode
    ) {
        coursePastelOf(
            seed = colorSeed, dark = useNightMode
        )
    }

    val isConflict = course is TodayClass.Conflict

    val containerColor = if (isConflict) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        pastel.container
    }

    val contentColor = if (isConflict) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        pastel.content
    }

    val shape = RoundedCornerShape(10.dp)

    val title = when (course) {
        is TodayClass.Single -> course.name

        is TodayClass.Conflict -> "冲突课程 (${course.data.size}门)"
    }

    val location = single?.location?.takeIf(String::isNotBlank)

    val cardModifier = modifier.applyIf(sharedBoundsKey) { key ->
        shareBoundsComposed(
            sharedContentState = rememberSharedContentState(
                key = key
            ), animatedVisibilityScope = LocalAnimatedContentScope.current
        )
    }.clip(shape).clickable(onClick = onClick)

    Card(
        modifier = cardModifier, shape = shape, colors = CardDefaults.cardColors(
            containerColor = containerColor, contentColor = contentColor
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        val progress = course.progress.collectAsState(null).value

        progress?.let {
            LinearProgressIndicator(
                progress = {
                    it.coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = contentColor,
                trackColor = contentColor.copy(
                    alpha = 0.15f
                )
            )
        }

        CourseCardText(
            title = title,
            location = location,
            contentColor = contentColor,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(
                horizontal = 5.dp, vertical = 4.dp
            )
        )
    }
}

@Composable
private fun CourseCardText(
    title: String, location: String?, contentColor: Color, modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val typographyMetrics = courseCardTypographyMetrics(maxWidth)
        val titleStyle = MaterialTheme.typography.labelMedium.copy(
            fontSize = typographyMetrics.titleFontSize,
            lineHeight = typographyMetrics.titleLineHeight
        )
        val locationStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = typographyMetrics.locationFontSize,
            lineHeight = typographyMetrics.locationLineHeight
        )

        Layout(
            modifier = Modifier.fillMaxSize(), content = {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (location != null) {
                    Text(
                        text = location,
                        style = locationStyle,
                        color = contentColor.copy(alpha = 0.75f),
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = location,
                        style = locationStyle,
                        color = contentColor.copy(alpha = 0.75f),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }) { measurables, constraints ->
            val candidateConstraints = Constraints(
                minWidth = 0,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
            val candidates = measurables.map {
                it.measure(candidateConstraints)
            }
            val title2 = candidates[0]
            val title1 = candidates[1]
            val location2 = candidates.getOrNull(2)
            val location1 = candidates.getOrNull(3)
            val availableHeight = if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                Int.MAX_VALUE
            }

            val layoutMode = selectCourseCardLayoutMode(
                availableHeight = availableHeight,
                title2Height = title2.height,
                title1Height = title1.height,
                location2Height = location2?.height,
                location1Height = location1?.height
            )

            val titlePlaceable = when (layoutMode) {
                CourseCardLayoutMode.Title2Location2, CourseCardLayoutMode.Title2Location1, CourseCardLayoutMode.Title2 -> title2

                CourseCardLayoutMode.Title1Location1, CourseCardLayoutMode.Title1 -> title1

                CourseCardLayoutMode.Hidden -> null
            }
            val locationPlaceable = when (layoutMode) {
                CourseCardLayoutMode.Title2Location2 -> location2
                CourseCardLayoutMode.Title2Location1, CourseCardLayoutMode.Title1Location1 -> location1

                CourseCardLayoutMode.Title2, CourseCardLayoutMode.Title1, CourseCardLayoutMode.Hidden -> null
            }
            val contentWidth = maxOf(
                titlePlaceable?.width ?: 0, locationPlaceable?.width ?: 0
            )
            val contentHeight = (titlePlaceable?.height ?: 0) + (locationPlaceable?.height ?: 0)
            val layoutWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                contentWidth.coerceAtLeast(constraints.minWidth)
            }
            val layoutHeight = if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                contentHeight.coerceAtLeast(constraints.minHeight)
            }

            layout(
                width = layoutWidth, height = layoutHeight
            ) {
                titlePlaceable?.placeRelative(0, 0)
                locationPlaceable?.placeRelative(
                    x = 0, y = titlePlaceable?.height ?: 0
                )
            }
        }
    }
}

private fun courseCardTypographyMetrics(
    contentWidth: Dp
): CourseCardTypographyMetrics {
    val widthProgress = ((contentWidth - 40.dp) / 40.dp).coerceIn(0f, 1f)
    val titleFontSize = lerp(10.sp, 20.sp, widthProgress)
    val locationFontSize = lerp(9.sp, 18.sp, widthProgress)

    return CourseCardTypographyMetrics(
        titleFontSize = titleFontSize,
        titleLineHeight = titleFontSize * 1.25f,
        locationFontSize = locationFontSize,
        locationLineHeight = locationFontSize * 1.25f
    )
}

private fun selectCourseCardLayoutMode(
    availableHeight: Int,
    title2Height: Int,
    title1Height: Int,
    location2Height: Int?,
    location1Height: Int?
): CourseCardLayoutMode {
    if (location2Height != null && location1Height != null) {
        return when {
            title2Height + location2Height <= availableHeight -> CourseCardLayoutMode.Title2Location2

            title2Height + location1Height <= availableHeight -> CourseCardLayoutMode.Title2Location1

            title1Height + location1Height <= availableHeight -> CourseCardLayoutMode.Title1Location1

            title2Height <= availableHeight -> CourseCardLayoutMode.Title2

            title1Height <= availableHeight -> CourseCardLayoutMode.Title1

            else -> CourseCardLayoutMode.Hidden
        }
    }

    return when {
        title2Height <= availableHeight -> CourseCardLayoutMode.Title2

        title1Height <= availableHeight -> CourseCardLayoutMode.Title1

        else -> CourseCardLayoutMode.Hidden
    }
}

private data class CourseCardTypographyMetrics(
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val locationFontSize: TextUnit,
    val locationLineHeight: TextUnit
)

private enum class CourseCardLayoutMode {
    Title2Location2, Title2Location1, Title1Location1, Title2, Title1, Hidden
}

private data class CoursePastel(
    val container: Color, val content: Color
)

private val LightCoursePalette = listOf(
    CoursePastel(Color(0xFFFDE7E9), Color(0xFF7E4A51)),
    CoursePastel(Color(0xFFFDEEE0), Color(0xFF7A5638)),
    CoursePastel(Color(0xFFFAF3D7), Color(0xFF6B5E33)),
    CoursePastel(Color(0xFFE3F3E6), Color(0xFF3F6B4C)),
    CoursePastel(Color(0xFFE2EFFA), Color(0xFF3E5F7A)),
    CoursePastel(Color(0xFFEDE9F9), Color(0xFF5B5286)),
    CoursePastel(Color(0xFFF9E8F2), Color(0xFF7A4A6B)),
    CoursePastel(Color(0xFFE0F4F1), Color(0xFF356860)),
)

private val DarkCoursePalette = listOf(
    CoursePastel(Color(0xFF4A3136), Color(0xFFF3C9CE)),
    CoursePastel(Color(0xFF47382B), Color(0xFFF1D3B8)),
    CoursePastel(Color(0xFF453F2A), Color(0xFFEDE3B4)),
    CoursePastel(Color(0xFF2C4032), Color(0xFFBFDFC6)),
    CoursePastel(Color(0xFF2A3A48), Color(0xFFC3D9EC)),
    CoursePastel(Color(0xFF393550), Color(0xFFD8D1F2)),
    CoursePastel(Color(0xFF452F3D), Color(0xFFEDC9DF)),
    CoursePastel(Color(0xFF27403C), Color(0xFFBFE3DD)),
)

private fun coursePastelOf(
    seed: Int, dark: Boolean
): CoursePastel {
    val palette = if (dark) {
        DarkCoursePalette
    } else {
        LightCoursePalette
    }

    return palette[(seed and Int.MAX_VALUE) % palette.size]
}

private data class TimelineRange(
    val start: LocalTime,
    val end: LocalTime
) {
    val durationMinutes: Int
        get() = (end.toSecondOfDay() - start.toSecondOfDay()) / 60

    fun marks(stepMinutes: Int): IntProgression =
        (start.toSecondOfDay() / 60) until (end.toSecondOfDay() / 60) step stepMinutes
}

private val TodayClass.segmentKey: CourseSegmentKey
    get() = CourseSegmentKey(
        recordId = (this as? TodayClass.Single)?.recordId,
        startTime = date.first,
        endTime = date.second
    )


private fun Int.toTimeLabel(): String {
    val hour = this / MinutesPerHour
    val minute = this % MinutesPerHour

    return buildString {
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
    }
}

private fun LocalTime.toTimeLabel(): String {
    return buildString {
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
    }
}

private const val MinutesPerHour = 60

private val CourseExpansionAnimationSpec = tween<Float>(
    durationMillis = 280
)

private data class CourseSegmentKey(
    val recordId: Long?, val startTime: LocalDateTime, val endTime: LocalDateTime
)
