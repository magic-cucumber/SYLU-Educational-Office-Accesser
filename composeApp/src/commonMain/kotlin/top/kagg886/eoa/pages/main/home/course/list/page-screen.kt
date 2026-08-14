package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.util.getTimeByLessonNumber
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
                nav.navigate(CourseConflictRoute(it.weekNumber, it.dayOfWeek, it.periodOfDay))
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
        onCourseConflictClicked = { dayOfWeek, periodOfDay ->
            model.navigateToConflictDetail(index + 1, dayOfWeek, periodOfDay)
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
    onCourseItemClicked: (CourseAndRecord) -> Unit,
    onCourseConflictClicked: (dayOfWeek: Int, periodOfDay: Int) -> Unit,
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
    onCourseItemClicked: (CourseAndRecord) -> Unit,
    onCourseConflictClicked: (dayOfWeek: Int, periodOfDay: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val lessonIndicatorWidth = when {
            maxWidth < 600.dp -> 56.dp
            maxWidth < 840.dp -> 68.dp
            else -> 84.dp
        }
        val expandedLessonIndicator = lessonIndicatorWidth >= 72.dp

        Row {
            Column(Modifier.width(lessonIndicatorWidth)) {
                // 第一个格子显示本周首天的月份
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight / 2)
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.thisWeekStartDate.month.number}月",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                LessonIndicatorColumn(expanded = expandedLessonIndicator)
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

            for (i in visibleDays) {
                Column(Modifier.weight(1f)) {
                    val date = state.thisWeekStartDate.plus(i - 1, DateTimeUnit.DAY)
                    val isCurrentDay = date == state.currentDate

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight / 2)
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        tonalElevation = if (isCurrentDay) 3.dp else 1.dp,
                        color = if (isCurrentDay) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            val dayOfWeek = when (i) {
                                1 -> "周一"
                                2 -> "周二"
                                3 -> "周三"
                                4 -> "周四"
                                5 -> "周五"
                                6 -> "周六"
                                7 -> "周日"
                                else -> error("unreachable")
                            }

                            Text(
                                text = date.format(OnlyMonthAndDayFormat),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isCurrentDay) FontWeight.Medium else FontWeight.Normal,
                                color = if (isCurrentDay)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentDay)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider()

                    Box(modifier = Modifier.height(columnHeight)) {
                        for ((next, course) in state.currentWeekCourse[i].orEmpty()) {
                            val topOffset = cardHeight * (next - 1) // 节次从1开始
                            val scheme = MaterialTheme.colorScheme
                            val basicColor = remember(course,useNightMode) {
                                when {
                                    course.hasConflict -> scheme.errorContainer
                                    !useNightMode -> Color.hsv(
                                        hue = Random(course.asNoConflict.course.name.hashCode()).nextInt(36000) / 100.0f,
                                        saturation = 0.1412f,
                                        value = 1f
                                    )
                                    else -> Color.hsv(
                                        hue = Random(course.asNoConflict.course.name.hashCode()).nextInt(36000) / 100.0f,
                                        saturation = 0.3038f,
                                        value = 0.3039f
                                    )
                                }
                            }

                            ElevatedCard(
                                modifier = Modifier
                                    .offset(y = topOffset)
                                    .fillMaxWidth()
                                    .height(cardHeight)
                                    .padding(horizontal = cardPadding, vertical = cardPadding)
                                    .applyIf(!course.hasConflict) {
                                        shareElementComposed(
                                            sharedContentState = rememberSharedContentState(key = "list-course-to-detail-${course.asNoConflict.record.id}"),
                                            animatedVisibilityScope = LocalAnimatedContentScope.current
                                        )
                                    }
                                    .clip(CardDefaults.shape)
                                    .clickable {
                                        if (course.hasConflict) {
                                            onCourseConflictClicked(
                                                course[0].record.dayOfWeek,
                                                course[0].record.periodOfDay
                                            )
                                            return@clickable
                                        }
                                        onCourseItemClicked(course.asNoConflict)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = 2.dp
                                ),
                                colors = if (course.hasConflict) {
                                    CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    )
                                } else {
                                    CardDefaults.elevatedCardColors(
                                        //使用hsv避免过于鲜艳的颜色
                                        containerColor = basicColor,
                                    )
                                }
                            ) {
                                if (course.hasConflict) {
                                    Text(
                                        "冲突课程 (${course.size}) 门",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 6.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        "点击查看",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                            .padding(bottom = 4.dp)
                                    )
                                } else {
                                    Text(
                                        course.asNoConflict.course.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        course.asNoConflict.course.classroomName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                            .padding(bottom = 4.dp)
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
private fun LessonIndicatorColumn(expanded: Boolean) {
    Column(modifier = Modifier.height(columnHeight)) {
        for (lessonNumber in 1..12) {
            LessonIndicatorItem(
                lessonNumber = lessonNumber,
                expanded = expanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .padding(cardPadding)
            )
        }
    }
}

@Composable
private fun LessonIndicatorItem(
    lessonNumber: Int,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    val (start, end) = getTimeByLessonNumber(lessonNumber)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {
        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                LessonNumberPill(lessonNumber)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = start.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = end.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LessonNumberPill(lessonNumber)
                Text(
                    text = "${start}\n${end}",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun LessonNumberPill(lessonNumber: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(
            modifier = Modifier.size(width = 28.dp, height = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = lessonNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private val OnlyMonthAndDayFormat = LocalDate.Format {
    monthNumber()
    char('-')
    day()
}

val cardHeight = 110.dp  // 每节课高度
val cardPadding = 2.dp  // 每节课之间的空隙调整为更小的值，使卡片更紧凑
val columnHeight = 12 * (cardHeight + cardPadding) // 课程表总高度
