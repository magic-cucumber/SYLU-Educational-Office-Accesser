package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.zoomBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.roundToInt

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
        scale = scale,
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
    scale: Float,
    useNightMode: Boolean,
    hideWeekendCourse: Boolean,
    onCourseItemClicked: (TodayClass.Single) -> Unit,
    onCourseConflictClicked: (LocalDateTime, LocalDateTime) -> Unit,
    onZoomChange: (Float) -> Unit,
) {
    when (state) {
        is CoursePageState.Failed -> {
            ErrorPage(title = {
                Text("加载课表失败")
            }, message = {
                Text(state.msg)
            })
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
                scale = scale,
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
    scale: Float,
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

    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val currentScale by rememberUpdatedState(scale)
    val transformableState = rememberTransformableState {
            _,
            zoomChange,
            _,
            _,
        ->
        currentOnZoomChange(zoomChange)
    }

    val coroutineScope = rememberCoroutineScope()
    val zoomEnabled = expandedCourseKey == null
    val density = LocalDensity.current
    val timelineDividerOffset = 4.dp + with(density) {
        MaterialTheme.typography.labelSmall.lineHeight.toDp()
    } / 2
    val timelineScaleOriginPx = with(density) {
        (ComponentDefault.HeaderHeight + timelineDividerOffset).toPx()
    }
    var zoomAnchorY by remember {
        mutableStateOf<Float?>(null)
    }
    var pendingTouchScale by remember {
        mutableStateOf<Float?>(null)
    }
    var scrollCompensatedScale by remember {
        mutableStateOf(scale)
    }

    LaunchedEffect(scale) {
        val anchorY = zoomAnchorY
        val pendingScale = pendingTouchScale
        val previousScale = scrollCompensatedScale

        if (anchorY != null && pendingScale != null && scale != previousScale) {
            val scaleChange = scale / previousScale
            val currentScroll = scrollState.value.toFloat()
            val targetScroll =
                timelineScaleOriginPx + (currentScroll + anchorY - timelineScaleOriginPx) * scaleChange - anchorY

            scrollState.scrollTo(targetScroll.roundToInt())
        }

        scrollCompensatedScale = scale

        if (pendingScale != null && pendingTouchScale == pendingScale && abs(pendingScale - scale) <= ScaleThresholdEpsilon) {
            pendingTouchScale = null
            zoomAnchorY = null
        }
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

            // 在 Initial 阶段拦截 Ctrl + 滚轮，
            // 避免同时触发纵向滚动。
            .pointerInput(transformableState, zoomEnabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(
                            pass = PointerEventPass.Initial
                        )

                        // 只处理 Ctrl + 滚轮。
                        // Shift + 滚轮以及普通滚轮全部交给外层处理。
                        if (event.type != PointerEventType.Scroll || !event.keyboardModifiers.isCtrlPressed) {
                            continue
                        }

                        val delta = event.changes.firstOrNull()?.scrollDelta ?: continue

                        val wheelDelta = when {
                            delta.y != 0f -> delta.y
                            delta.x != 0f -> delta.x
                            else -> continue
                        }

                        // 只有 Ctrl + 滚轮才消费。
                        event.changes.forEach {
                            it.consume()
                        }

                        if (!zoomEnabled) {
                            continue
                        }

                        val zoomFactor = if (wheelDelta < 0f) {
                            MouseWheelZoomStep
                        } else {
                            1f / MouseWheelZoomStep
                        }

                        coroutineScope.launch {
                            transformableState.zoomBy(zoomFactor)
                        }
                    }
                }
            }

            // 在 Initial 阶段接管双指手势，避免 Card 点击或滚动先消费事件，
            // 导致捏合被取消。单指事件仍交给点击、纵向滚动和周切换。
            .pointerInput(zoomEnabled) {
                if (!zoomEnabled) {
                    return@pointerInput
                }

                awaitPointerEventScope {
                    while (true) {
                        var isMultiTouchGesture = false
                        var pastTouchSlop = false
                        var accumulatedZoom = 1f
                        var gestureScale = currentScale
                        var event = awaitPointerEvent(
                            pass = PointerEventPass.Initial
                        )

                        do {
                            if (event.changes.count { it.pressed } >= 2) {
                                isMultiTouchGesture = true
                            }

                            if (isMultiTouchGesture) {
                                val zoomChange = event.calculateZoom()

                                if (!pastTouchSlop) {
                                    accumulatedZoom *= zoomChange
                                    val centroidSize = event.calculateCentroidSize(
                                        useCurrent = false
                                    )
                                    pastTouchSlop =
                                        abs(1f - accumulatedZoom) * centroidSize > viewConfiguration.touchSlop
                                }

                                // 第二个触点出现后立刻取消 Card press 和滚动竞争；
                                // 即使先抬起一根手指，也持续消费到本轮手势结束。
                                event.changes.forEach {
                                    it.consume()
                                }

                                if (pastTouchSlop && zoomChange != 1f) {
                                    val targetScale = (gestureScale * zoomChange).coerceIn(
                                        0.5f, MaxTimelineScale
                                    )
                                    val effectiveZoomChange = targetScale / gestureScale

                                    if (effectiveZoomChange != 1f) {
                                        zoomAnchorY = event.calculateCentroid().y
                                        pendingTouchScale = targetScale
                                        gestureScale = targetScale
                                        currentOnZoomChange(effectiveZoomChange)
                                    }
                                }
                            }

                            if (event.changes.none { it.pressed }) {
                                break
                            }

                            event = awaitPointerEvent(
                                pass = PointerEventPass.Initial
                            )
                        } while (true)
                    }
                }
            }

            .verticalScroll(scrollState)

            .miuiLongShotSupport(
                enabled = longShotEnabled, scrollState = scrollState
            )) {
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

        val timelineRange = remember(currentWeekCourse) {
            calculateTimelineRange(
                currentWeekCourse.values.flatten()
            )
        }

        // 1x = 2.4dp/min; page-level scale controls the full layout height.
        val minuteHeight = 2.4.dp * scale
        val tickIntervalMinutes = when {
            scale >= MaxTimelineScale - ScaleThresholdEpsilon -> 15
            scale >= 1f -> 30
            else -> 60
        }
        val timelineHeight = timelineRange.durationMinutes * minuteHeight
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
                    startTime = timelineRange.startMinute.toLocalTime(),
                    endTime = timelineRange.endMinute.toLocalTime(),
                    allowIsoWeekNumber = days,
                    timelineWidth = timeAxisWidth,
                    modifier = Modifier.fillMaxWidth().height(timelineHeight).drawBehind {
                        for (minute in timelineRange.marks(tickIntervalMinutes)) {
                            val y =
                                (minute - timelineRange.startMinute).toFloat() / timelineRange.durationMinutes * size.height
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
                        TimeAxis(timelineRange, tickIntervalMinutes)
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
private fun CourseTimelineScope.TimeAxis(range: TimelineRange, tickIntervalMinutes: Int) {
    val halfLabelHeight = with(LocalDensity.current) {
        MaterialTheme.typography.labelSmall.lineHeight.toDp() / 2
    }
    for (minute in range.marks(tickIntervalMinutes)) {
        val isHourMark = minute % MinutesPerHour == 0
        Text(
            text = minute.toTimeLabel(),
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = offsetOf(minute.toLocalTime()) - halfLabelHeight),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHourMark) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isHourMark) 0.8f else 0.55f),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun interpolateTime(start: LocalTime, end: LocalTime, progress: Float): LocalTime {
    val from = start.toNanosecondOfDay()
    val to = end.toNanosecondOfDay()
    return LocalTime.fromNanosecondOfDay(from + ((to - from) * progress.toDouble()).roundToLong())
}

// An enclosing hour can be 24:00; LocalTime represents the final nanosecond of that day instead.
private fun Int.toLocalTime(): LocalTime =
    LocalTime.fromNanosecondOfDay((toLong() * 60_000_000_000L).coerceAtMost(86_399_999_999_999L))

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
    val startMinute: Int, val endMinute: Int
) {
    val durationMinutes: Int
        get() = endMinute - startMinute

    fun marks(stepMinutes: Int): IntProgression = startMinute until endMinute step stepMinutes
}

private fun calculateTimelineRange(
    courses: List<TodayClass>
): TimelineRange {
    val earliestMinute = courses.minOfOrNull {
        if (it is TodayClass.Single) {
            minOf(it.startMinute, it.fullStartMinute)
        } else {
            it.startMinute
        }
    } ?: DefaultStartMinute

    val latestMinute = courses.maxOfOrNull {
        if (it is TodayClass.Single) {
            maxOf(it.endMinute, it.fullEndMinute)
        } else {
            it.endMinute
        }
    } ?: DefaultEndMinute

    val startMinute = minOf(DefaultStartMinute, earliestMinute).roundDownToHour()

    val endMinute = maxOf(DefaultEndMinute, latestMinute).roundUpToHour()

    return TimelineRange(
        startMinute = startMinute, endMinute = endMinute
    )
}

private val TodayClass.startMinute: Int
    get() = date.first.hour * MinutesPerHour + date.first.minute

private val TodayClass.endMinute: Int
    get() = date.second.hour * MinutesPerHour + date.second.minute

private val TodayClass.Single.fullStartMinute: Int
    get() = fullDate.first.hour * MinutesPerHour + fullDate.first.minute

private val TodayClass.Single.fullEndMinute: Int
    get() = fullDate.second.hour * MinutesPerHour + fullDate.second.minute

private val TodayClass.segmentKey: CourseSegmentKey
    get() = CourseSegmentKey(
        recordId = (this as? TodayClass.Single)?.recordId,
        startTime = date.first,
        endTime = date.second
    )

private fun Int.roundDownToHour(): Int = this / MinutesPerHour * MinutesPerHour

private fun Int.roundUpToHour(): Int = (this + MinutesPerHour - 1) / MinutesPerHour * MinutesPerHour

private fun Int.toTimeLabel(): String {
    val hour = this / MinutesPerHour
    val minute = this % MinutesPerHour

    return buildString {
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
    }
}

private const val MinutesPerHour = 60

private const val DefaultStartMinute = 8 * MinutesPerHour

private const val DefaultEndMinute = 22 * MinutesPerHour

/*
 * 时间轴缩放：
 *
 * 0.5x -> 60 分钟刻度
 * 1x -> 30 分钟刻度
 * 2x -> 15 分钟刻度
 */
private const val MaxTimelineScale = 2f

/*
 * 避免浮点数刚好为 1.999999 时
 * 无法进入 15 分钟刻度。
 */
private const val ScaleThresholdEpsilon = 0.001f

/*
 * Ctrl + 每个滚轮事件缩放约 5%。
 */
private const val MouseWheelZoomStep = 1.05f

private val CourseExpansionAnimationSpec = tween<Float>(
    durationMillis = 280
)

private data class CourseSegmentKey(
    val recordId: Long?, val startTime: LocalDateTime, val endTime: LocalDateTime
)
