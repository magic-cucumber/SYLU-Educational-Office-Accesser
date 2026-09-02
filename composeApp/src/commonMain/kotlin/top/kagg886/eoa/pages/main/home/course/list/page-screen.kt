package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.zoomBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalNavController
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

@Composable
fun CoursePageListScreen(
    index: Int,
    isCurrentPage: Boolean,
    scale: Float,
    onZoomChange: (Float) -> Unit,
) {
    val mainViewModel = mainViewModelOrNull() ?: return
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CoursePageViewModel>(
        key = "${index * 31 + syncState.toViewModelKey().hashCode()}"
    ) {
        CoursePageViewModel(syncState, index + 1, mainViewModel.database)
    }

    val nav = LocalNavController.current

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

    val state by model.collectAsState()

    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val theme by rootState.theme.collectAsState()
    val hideWeekendCourse by rootState.hideWeekendCourse.collectAsState()

    val systemNight = isSystemInDarkTheme()
    val useNightMode = remember(theme, systemNight) {
        when (theme) {
            AppSettingsMMKVType.AppTheme.Dark -> true
            AppSettingsMMKVType.AppTheme.Light -> false
            AppSettingsMMKVType.AppTheme.SystemDefault -> systemNight
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
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
            val scrollState = rememberScrollState()

            CoursePageScreenSuccess(
                thisWeekStartDate = state.thisWeekStartDate,
                currentDate = state.currentDate,
                currentWeekCourse = state.currentWeekCourse,
                scale = scale,
                useNightMode = useNightMode,
                hideWeekendCourse = hideWeekendCourse,
                scrollState = scrollState,
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
    scrollState: androidx.compose.foundation.ScrollState,
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

    fun expandCourse(course: TodayClass.Single) {
        expandedCourseKey = course.segmentKey
        coroutineScope.launch {
            expansionProgress.animateTo(
                targetValue = 1f,
                animationSpec = CourseExpansionAnimationSpec
            )
        }
    }

    fun dismissExpandedCourse() {
        val dismissingKey = expandedCourseKey ?: return
        coroutineScope.launch {
            expansionProgress.animateTo(
                targetValue = 0f,
                animationSpec = CourseExpansionAnimationSpec
            )
            if (expandedCourseKey == dismissingKey) {
                expandedCourseKey = null
            }
        }
    }

    fun handleCourseClicked(course: TodayClass) {
        val currentExpandedKey = expandedCourseKey

        if (currentExpandedKey != null) {
            if (
                course is TodayClass.Single &&
                course.segmentKey == currentExpandedKey
            ) {
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
                    course.date.first,
                    course.date.second
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

            // 在 Initial 阶段拦截 Shift + 滚轮，
            // 避免同时触发纵向滚动。
            .pointerInput(transformableState, zoomEnabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(
                            pass = PointerEventPass.Initial
                        )

                        // 只处理 Ctrl + 滚轮。
                        // Shift + 滚轮以及普通滚轮全部交给外层处理。
                        if (
                            event.type != PointerEventType.Scroll ||
                            !event.keyboardModifiers.isCtrlPressed
                        ) {
                            continue
                        }

                        val delta = event.changes
                            .firstOrNull()
                            ?.scrollDelta
                            ?: continue

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

            .verticalScroll(scrollState)
            // 移动端双指捏合。
            //
            // canPan = false 很重要：
            // 不让 transformable 抢占单指纵向滚动。
            .transformable(
                state = transformableState,
                canPan = { false },
                lockRotationOnZoomPan = true,
                enabled = zoomEnabled
            )

            .miuiLongShotSupport(
                enabled = longShotEnabled,
                scrollState = scrollState
            )
    ) {
        val timeAxisWidth = when {
            maxWidth < 600.dp -> 48.dp
            maxWidth < 840.dp -> 56.dp
            else -> 64.dp
        }

        val visibleDays = remember(hideWeekendCourse, currentWeekCourse) {
            if (
                hideWeekendCourse &&
                currentWeekCourse[6].isNullOrEmpty() &&
                currentWeekCourse[7].isNullOrEmpty()
            ) {
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

        /*
         * 1x = 1.2dp / 分钟
         * 2x = 2.4dp / 分钟
         * 3x = 3.6dp / 分钟
         */
        val minuteHeight = BaseMinuteHeight * scale

        val tickIntervalMinutes = when {
            scale >= MaxTimelineScale - ScaleThresholdEpsilon -> 15
            scale >= HalfHourScale -> 30
            else -> 60
        }

        val timelineDividerOffset = TimeLabelTopPadding +
                with(LocalDensity.current) {
                    MaterialTheme.typography.labelSmall.lineHeight.toDp()
                } / 2

        val timelineHeight =
            timelineRange.durationMinutes * minuteHeight +
                    timelineDividerOffset

        Column {
            Row(
                modifier = Modifier.height(CalendarHeaderHeight)
            ) {
                MonthHeader(
                    month = thisWeekStartDate.month.number,
                    modifier = Modifier
                        .width(timeAxisWidth)
                        .fillMaxHeight()
                )

                for (dayOfWeek in visibleDays) {
                    val date = thisWeekStartDate.plus(
                        dayOfWeek - 1,
                        DateTimeUnit.DAY
                    )

                    DayHeader(
                        date = date,
                        dayOfWeek = dayOfWeek,
                        isCurrentDay = date == currentDate,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            Row {
                TimeAxis(
                    range = timelineRange,
                    minuteHeight = minuteHeight,
                    tickIntervalMinutes = tickIntervalMinutes,
                    modifier = Modifier
                        .width(timeAxisWidth)
                        .height(timelineHeight)
                )

                for (dayOfWeek in visibleDays) {
                    DayTimeline(
                        courses = currentWeekCourse[dayOfWeek].orEmpty(),
                        range = timelineRange,
                        minuteHeight = minuteHeight,
                        tickIntervalMinutes = tickIntervalMinutes,
                        dividerOffset = timelineDividerOffset,
                        useNightMode = useNightMode,
                        expandedCourseKey = expandedCourseKey,
                        expansionProgress = expansionProgress.value,
                        onCourseClicked = ::handleCourseClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(timelineHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.height(with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 4.dp + 28.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.5f
            ),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${month}月",
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    dayOfWeek: Int,
    isCurrentDay: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfWeekName(dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrentDay) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrentDay) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentDay) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = if (isCurrentDay) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimeAxis(
    range: TimelineRange,
    minuteHeight: Dp,
    tickIntervalMinutes: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        for (minute in range.marks(tickIntervalMinutes)) {
            val isHourMark = minute % MinutesPerHour == 0

            Text(
                text = minute.toTimeLabel(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = range.offsetOf(
                            minute = minute,
                            minuteHeight = minuteHeight
                        ) + TimeLabelTopPadding
                    ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isHourMark) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isHourMark) {
                        0.8f
                    } else {
                        0.55f
                    }
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DayTimeline(
    courses: List<TodayClass>,
    range: TimelineRange,
    minuteHeight: Dp,
    tickIntervalMinutes: Int,
    dividerOffset: Dp,
    useNightMode: Boolean,
    expandedCourseKey: CourseSegmentKey?,
    expansionProgress: Float,
    onCourseClicked: (TodayClass) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedCourses = remember(courses) {
        courses.sortedBy { it.startMinute }
    }

    Box(modifier = modifier) {
        for (minute in range.marks(tickIntervalMinutes)) {
            val isHourMark = minute % MinutesPerHour == 0

            HorizontalDivider(
                modifier = Modifier
                    .offset(
                        y = range.offsetOf(
                            minute = minute,
                            minuteHeight = minuteHeight
                        ) + dividerOffset
                    )
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (isHourMark) {
                        DividerAlpha
                    } else {
                        MinorDividerAlpha
                    }
                )
            )
        }

        for (course in sortedCourses) {
            key(course.segmentKey) {
                val isExpanded =
                    course is TodayClass.Single &&
                            course.segmentKey == expandedCourseKey

                val collapsedTop = range.offsetOf(
                    minute = course.startMinute,
                    minuteHeight = minuteHeight
                ) + dividerOffset
                val collapsedHeight = range.heightOf(
                    courseStartMinute = course.startMinute,
                    courseEndMinute = course.endMinute,
                    minuteHeight = minuteHeight
                )

                val expandedTop = if (course is TodayClass.Single) {
                    range.offsetOf(
                        minute = course.fullStartMinute,
                        minuteHeight = minuteHeight
                    ) + dividerOffset
                } else {
                    collapsedTop
                }
                val expandedHeight = if (course is TodayClass.Single) {
                    range.heightOf(
                        courseStartMinute = course.fullStartMinute,
                        courseEndMinute = course.fullEndMinute,
                        minuteHeight = minuteHeight
                    )
                } else {
                    collapsedHeight
                }

                CourseCalendarCard(
                    course = course,
                    useNightMode = useNightMode,
                    onClick = {
                        onCourseClicked(course)
                    },
                    modifier = Modifier
                        .zIndex(
                            if (isExpanded) {
                                ExpandedCourseZIndex
                            } else {
                                0f
                            }
                        )
                        .offset(
                            y = if (isExpanded) {
                                lerp(
                                    collapsedTop,
                                    expandedTop,
                                    expansionProgress
                                )
                            } else {
                                collapsedTop
                            }
                        )
                        .fillMaxWidth()
                        .height(
                            if (isExpanded) {
                                lerp(
                                    collapsedHeight,
                                    expandedHeight,
                                    expansionProgress
                                )
                            } else {
                                collapsedHeight
                            }
                        )
                        .padding(
                            horizontal = CourseHorizontalPadding,
                            vertical = CourseVerticalPadding
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CourseCalendarCard(
    course: TodayClass,
    useNightMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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

    val colorSeed = single
        ?.name
        .orEmpty()
        .hashCode()

    val pastel = remember(
        colorSeed,
        useNightMode
    ) {
        coursePastelOf(
            seed = colorSeed,
            dark = useNightMode
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

    val density = LocalDensity.current
    val courseNameTextStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = with(density) { CourseNameFontSize.toSp() },
        lineHeight = with(density) { CourseTextLineHeight.toSp() },
        letterSpacing = with(density) { CourseTextLetterSpacing.toSp() }
    )
    val courseLocationTextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = with(density) { CourseLocationFontSize.toSp() },
        lineHeight = with(density) { CourseTextLineHeight.toSp() },
        letterSpacing = with(density) { CourseTextLetterSpacing.toSp() }
    )

    var showCourseName by remember(course) {
        mutableStateOf(false)
    }
    var showCourseLocation by remember(course) {
        mutableStateOf(course !is TodayClass.Single)
    }

    val cardModifier = modifier
        .applyIf(sharedBoundsKey) { key ->
            shareBoundsComposed(
                sharedContentState = rememberSharedContentState(
                    key = key
                ),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            )
        }
        .clip(shape)
        .clickable(onClick = onClick)

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        val progress = course.progress
            .collectAsState(null)
            .value

        progress?.let {
            LinearProgressIndicator(
                progress = {
                    it.coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = contentColor,
                trackColor = contentColor.copy(
                    alpha = 0.15f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 5.dp,
                    vertical = 4.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = when (course) {
                    is TodayClass.Single -> course.name

                    is TodayClass.Conflict ->
                        "冲突课程 (${course.data.size}门)"
                },
                modifier = Modifier.alpha(
                    if (showCourseName) 1f else 0f
                ),
                style = courseNameTextStyle,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = {
                    showCourseName = it.size.height.toFloat() >=
                            it.multiParagraph.height
                }
            )

            if (course is TodayClass.Single) {
                Text(
                    text = course.location,
                    modifier = Modifier.alpha(
                        if (showCourseLocation) 1f else 0f
                    ),
                    style = courseLocationTextStyle,
                    color = contentColor.copy(
                        alpha = 0.75f
                    ),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = {
                        showCourseLocation = it.size.height.toFloat() >=
                                it.multiParagraph.height
                    }
                )
            }
        }
    }
}

private data class CoursePastel(
    val container: Color,
    val content: Color
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
    seed: Int,
    dark: Boolean
): CoursePastel {
    val palette = if (dark) {
        DarkCoursePalette
    } else {
        LightCoursePalette
    }

    return palette[
        (seed and Int.MAX_VALUE) % palette.size
    ]
}

private data class TimelineRange(
    val startMinute: Int,
    val endMinute: Int
) {
    val durationMinutes: Int
        get() = endMinute - startMinute

    fun marks(stepMinutes: Int): IntProgression =
        startMinute until endMinute step stepMinutes

    fun offsetOf(
        minute: Int,
        minuteHeight: Dp
    ): Dp {
        return (minute - startMinute) * minuteHeight
    }

    fun heightOf(
        courseStartMinute: Int,
        courseEndMinute: Int,
        minuteHeight: Dp
    ): Dp {
        val visibleStart = courseStartMinute.coerceAtLeast(
            startMinute
        )

        val visibleEnd = courseEndMinute.coerceAtMost(
            endMinute
        )

        return (visibleEnd - visibleStart)
            .coerceAtLeast(1) * minuteHeight
    }
}

private fun calculateTimelineRange(
    courses: List<TodayClass>
): TimelineRange {
    val earliestMinute =
        courses.minOfOrNull {
            if (it is TodayClass.Single) {
                minOf(it.startMinute, it.fullStartMinute)
            } else {
                it.startMinute
            }
        }
            ?: DefaultStartMinute

    val latestMinute =
        courses.maxOfOrNull {
            if (it is TodayClass.Single) {
                maxOf(it.endMinute, it.fullEndMinute)
            } else {
                it.endMinute
            }
        }
            ?: DefaultEndMinute

    val startMinute =
        minOf(DefaultStartMinute, earliestMinute)
            .roundDownToHour()

    val endMinute =
        maxOf(DefaultEndMinute, latestMinute)
            .roundUpToHour()

    return TimelineRange(
        startMinute = startMinute,
        endMinute = endMinute
    )
}

private val TodayClass.startMinute: Int
    get() =
        date.first.hour * MinutesPerHour +
                date.first.minute

private val TodayClass.endMinute: Int
    get() =
        date.second.hour * MinutesPerHour +
                date.second.minute

private val TodayClass.Single.fullStartMinute: Int
    get() =
        fullDate.first.hour * MinutesPerHour +
                fullDate.first.minute

private val TodayClass.Single.fullEndMinute: Int
    get() =
        fullDate.second.hour * MinutesPerHour +
                fullDate.second.minute

private val TodayClass.segmentKey: CourseSegmentKey
    get() = CourseSegmentKey(
        recordId = (this as? TodayClass.Single)?.recordId,
        startTime = date.first,
        endTime = date.second
    )

private fun Int.roundDownToHour(): Int =
    this / MinutesPerHour * MinutesPerHour

private fun Int.roundUpToHour(): Int =
    (this + MinutesPerHour - 1) /
            MinutesPerHour *
            MinutesPerHour

private fun Int.toTimeLabel(): String {
    val hour = this / MinutesPerHour
    val minute = this % MinutesPerHour

    return buildString {
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
    }
}

private fun dayOfWeekName(
    dayOfWeek: Int
): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> error(
        "Invalid day of week: $dayOfWeek"
    )
}

private const val MinutesPerHour = 60

private const val DefaultStartMinute =
    8 * MinutesPerHour

private const val DefaultEndMinute =
    22 * MinutesPerHour

/*
 * 时间轴缩放：
 *
 * 0.5x -> 60 分钟刻度
 * 1x -> 30 分钟刻度
 * 2x -> 15 分钟刻度
 */
private const val MinTimelineScale = 0.5f
private const val HalfHourScale = 1f
private const val MaxTimelineScale = 2f

/*
 * 避免浮点数刚好为 2.999999 时
 * 无法进入 15 分钟刻度。
 */
private const val ScaleThresholdEpsilon = 0.001f

/*
 * Shift + 每个滚轮事件缩放约 5%。
 */
private const val MouseWheelZoomStep = 1.05f

private val CourseExpansionAnimationSpec = tween<Float>(
    durationMillis = 280
)

private const val ExpandedCourseZIndex = 99999f

private val CalendarHeaderHeight = 64.dp

/*
 * 这是 1x 时的最小高度。
 *
 * 1x = 1.2dp/min
 * 2x = 2.4dp/min
 * 3x = 3.6dp/min
 */
private val BaseMinuteHeight = 2.4.dp

private val TimeLabelTopPadding = 4.dp
private val CourseNameFontSize = 12.dp
private val CourseLocationFontSize = 11.dp
private val CourseTextLineHeight = 16.dp
private val CourseTextLetterSpacing = 0.5.dp
private val CourseHorizontalPadding = 3.dp
private val CourseVerticalPadding = 1.dp

private const val DividerAlpha = 0.5f
private const val MinorDividerAlpha = 0.25f

private data class CourseSegmentKey(
    val recordId: Long?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)
