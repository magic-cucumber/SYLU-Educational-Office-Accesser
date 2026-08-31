package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
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
import top.kagg886.eoa.pages.main.home.summary.TodayClass
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareBoundsComposed
import kotlin.random.Random

@Composable
fun CoursePageListScreen(index: Int, courseListState: CourseListState.DataAccessible) {
    val mainViewModel = mainViewModelOrNull() ?: return
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CoursePageViewModel>(key = "${index * 31 + syncState.toViewModelKey().hashCode()}") {
        CoursePageViewModel(syncState, index + 1, mainViewModel.database)
    }
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is CoursePageSideEffect.NavigateToCourseDetail -> {
                nav.navigate(CourseDetailRoute(it.recordId))
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
        index = index,
        state = state,
        courseListState = courseListState,
        useNightMode = useNightMode,
        hideWeekendCourse = hideWeekendCourse,
        onCourseItemClicked = {
            model.navigateToCourseDetail(it)
        },
        onCourseConflictClicked = { startTime, endTime ->
            model.navigateToConflictDetail(startTime, endTime)
        }
    )
}

@Composable
private fun CoursePageScreenContent(
    index: Int,
    state: CoursePageState,
    courseListState: CourseListState.DataAccessible,
    useNightMode: Boolean,
    hideWeekendCourse: Boolean,
    onCourseItemClicked: (TodayClass.Single) -> Unit,
    onCourseConflictClicked: (LocalDateTime, LocalDateTime) -> Unit,
) {
    when (state) {
        is CoursePageState.Failed -> {
            ErrorPage(
                title = {
                    Text(text = "加载课表失败")
                },
                message = {
                    Text(text = state.msg)
                }
            )
        }

        is CoursePageState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("正在加载课表，请稍等。")
                }
            }
        }

        is CoursePageState.Success -> {
            val scroll = rememberScrollState()

            CoursePageScreenSuccess(
                state = state,
                useNightMode = useNightMode,
                hideWeekendCourse = hideWeekendCourse,
                onCourseItemClicked = onCourseItemClicked,
                onCourseConflictClicked = onCourseConflictClicked,
                modifier = Modifier.fillMaxSize().verticalScroll(scroll).miuiLongShotSupport(enabled = courseListState.state.currentPage == index, scrollState = scroll)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CoursePageScreenSuccess(
    state: CoursePageState.Success,
    useNightMode: Boolean,
    hideWeekendCourse: Boolean,
    onCourseItemClicked: (TodayClass.Single) -> Unit,
    onCourseConflictClicked: (LocalDateTime, LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val timeAxisWidth = when {
            maxWidth < 600.dp -> 48.dp
            maxWidth < 840.dp -> 56.dp
            else -> 64.dp
        }
        val visibleDays = remember(hideWeekendCourse, state.currentWeekCourse) {
            if (
                hideWeekendCourse &&
                state.currentWeekCourse[6].isNullOrEmpty() &&
                state.currentWeekCourse[7].isNullOrEmpty()
            ) {
                1..5
            } else {
                1..7
            }
        }
        val timelineRange = remember(state.currentWeekCourse) {
            calculateTimelineRange(state.currentWeekCourse.values.flatten())
        }
        val timelineHeight = timelineRange.durationMinutes * MinuteHeight

        Column {
            Row(modifier = Modifier.height(CalendarHeaderHeight)) {
                MonthHeader(
                    month = state.thisWeekStartDate.month.number,
                    modifier = Modifier
                        .width(timeAxisWidth)
                        .fillMaxHeight()
                )
                for (dayOfWeek in visibleDays) {
                    val date = state.thisWeekStartDate.plus(dayOfWeek - 1, DateTimeUnit.DAY)
                    DayHeader(
                        date = date,
                        dayOfWeek = dayOfWeek,
                        isCurrentDay = date == state.currentDate,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            HorizontalDivider()

            Row {
                TimeAxis(
                    range = timelineRange,
                    modifier = Modifier
                        .width(timeAxisWidth)
                        .height(timelineHeight)
                )
                for (dayOfWeek in visibleDays) {
                    val date = state.thisWeekStartDate.plus(dayOfWeek - 1, DateTimeUnit.DAY)
                    DayTimeline(
                        courses = state.currentWeekCourse[dayOfWeek].orEmpty(),
                        range = timelineRange,
                        isCurrentDay = date == state.currentDate,
                        useNightMode = useNightMode,
                        onCourseItemClicked = onCourseItemClicked,
                        onCourseConflictClicked = onCourseConflictClicked,
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
private fun MonthHeader(month: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "${month}月",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    Surface(
        modifier = modifier.padding(4.dp),
        color = if (isCurrentDay) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (isCurrentDay) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (isCurrentDay) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.format(OnlyMonthAndDayFormat),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dayOfWeekName(dayOfWeek),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimeAxis(range: TimelineRange, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        for (minute in range.hourMarks) {
            Text(
                text = minute.toTimeLabel(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = range.offsetOf(minute) + TimeLabelTopPadding),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    isCurrentDay: Boolean,
    useNightMode: Boolean,
    onCourseItemClicked: (TodayClass.Single) -> Unit,
    onCourseConflictClicked: (LocalDateTime, LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedCourses = remember(courses) { courses.sortedBy { it.startMinute } }
    Box(
        modifier = modifier.background(
            if (isCurrentDay) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            } else {
                Color.Transparent
            }
        )
    ) {
        VerticalDivider(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
        )
        for (minute in range.hourMarks) {
            HorizontalDivider(
                modifier = Modifier
                    .offset(y = range.offsetOf(minute))
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        }
        for (course in sortedCourses) {
            CourseCalendarCard(
                course = course,
                useNightMode = useNightMode,
                onClick = {
                    when (course) {
                        is TodayClass.Single -> onCourseItemClicked(course)
                        is TodayClass.Conflict -> onCourseConflictClicked(
                            course.date.first,
                            course.date.second
                        )
                    }
                },
                modifier = Modifier
                    .offset(y = range.offsetOf(course.startMinute))
                    .fillMaxWidth()
                    .height(range.heightOf(course))
                    .padding(horizontal = CourseHorizontalPadding, vertical = CourseVerticalPadding)
            )
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
    val recordId = single?.recordId
    val colorSeed = single?.name.orEmpty().hashCode()
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = remember(course, useNightMode, colorScheme) {
        when {
            course is TodayClass.Conflict -> colorScheme.errorContainer
            !useNightMode -> Color.hsv(
                hue = Random(colorSeed).nextInt(36000) / 100f,
                saturation = 0.1412f,
                value = 1f
            )
            else -> Color.hsv(
                hue = Random(colorSeed).nextInt(36000) / 100f,
                saturation = 0.3038f,
                value = 0.3039f
            )
        }
    }
    val shape = RoundedCornerShape(8.dp)
    val cardModifier = modifier
        .applyIf(recordId != null) {
            shareBoundsComposed(
                sharedContentState = rememberSharedContentState(
                    key = "list-course-to-detail-$recordId"
                ),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            )
        }
        .clip(shape)
        .clickable(onClick = onClick)

    ElevatedCard(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        val progress = course.progress.collectAsState(null).value
        progress?.let {
            LinearProgressIndicator(
                progress = { it.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (course) {
                    is TodayClass.Single -> course.name
                    is TodayClass.Conflict -> "冲突课程 (${course.data.size}门)"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (course is TodayClass.Single) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = course.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class TimelineRange(
    val startMinute: Int,
    val endMinute: Int
) {
    val durationMinutes: Int = endMinute - startMinute
    val hourMarks: IntProgression = startMinute until endMinute step MinutesPerHour

    fun offsetOf(minute: Int): Dp = (minute - startMinute) * MinuteHeight

    fun heightOf(course: TodayClass): Dp {
        val visibleStart = course.startMinute.coerceAtLeast(startMinute)
        val visibleEnd = course.endMinute.coerceAtMost(endMinute)
        return (visibleEnd - visibleStart).coerceAtLeast(1) * MinuteHeight
    }
}

private fun calculateTimelineRange(courses: List<TodayClass>): TimelineRange {
    val earliestMinute = courses.minOfOrNull { it.startMinute } ?: DefaultStartMinute
    val latestMinute = courses.maxOfOrNull { it.endMinute } ?: DefaultEndMinute
    val startMinute = minOf(DefaultStartMinute, earliestMinute).roundDownToHour()
    val endMinute = maxOf(DefaultEndMinute, latestMinute).roundUpToHour()
    return TimelineRange(startMinute = startMinute, endMinute = endMinute)
}

private val TodayClass.startMinute: Int
    get() = date.first.hour * MinutesPerHour + date.first.minute

private val TodayClass.endMinute: Int
    get() = date.second.hour * MinutesPerHour + date.second.minute

private fun Int.roundDownToHour(): Int = this / MinutesPerHour * MinutesPerHour

private fun Int.roundUpToHour(): Int =
    (this + MinutesPerHour - 1) / MinutesPerHour * MinutesPerHour

private fun Int.toTimeLabel(): String =
    "${(this / MinutesPerHour).toString().padStart(2, '0')}:00"

private fun dayOfWeekName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> error("Invalid day of week: $dayOfWeek")
}

private val OnlyMonthAndDayFormat = LocalDate.Format {
    monthNumber()
    char('-')
    day()
}

private const val MinutesPerHour = 60
private const val DefaultStartMinute = 8 * MinutesPerHour
private const val DefaultEndMinute = 22 * MinutesPerHour

private val CalendarHeaderHeight = 64.dp
private val MinuteHeight = 1.2.dp
private val TimeLabelTopPadding = 4.dp
private val CourseHorizontalPadding = 2.dp
private val CourseVerticalPadding = 1.dp
