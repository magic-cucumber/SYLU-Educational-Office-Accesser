package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.datetime.plus
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.util.getTimeByLessonNumber

@Composable
fun CoursePageListScreen(
    index: Int
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CoursePageViewModel>(key = "${index * 31 + syncState.hashCode()}") {
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

    CoursePageScreenContent(
        state = state,
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
    state: CoursePageState,
    onCourseItemClicked: (CourseAndRecord) -> Unit,
    onCourseConflictClicked: (dayOfWeek: Int, periodOfDay: Int) -> Unit
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
            CoursePageScreenSuccess(
                state = state,
                onCourseItemClicked = onCourseItemClicked,
                onCourseConflictClicked = onCourseConflictClicked,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CoursePageScreenSuccess(
    state: CoursePageState.Success,
    onCourseItemClicked: (CourseAndRecord) -> Unit,
    onCourseConflictClicked: (dayOfWeek: Int, periodOfDay: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.verticalScroll(rememberScrollState())) {
        Column(Modifier.weight(1f)) {
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
                        text = "${state.thisWeekStartDate.monthNumber}月",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // 课节指示器
            Column(modifier = Modifier.height(columnHeight)) {
                for (i in 1..12) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(cardHeight).padding(cardPadding)
                    ) {
                        Text(
                            text = "$i",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                        )

                        val (start, end) = getTimeByLessonNumber(i)
                        Text(
                            text = "$start\n---\n$end",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                }
            }
        }
        for (i in 1..7) {
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

                        ElevatedCard(
                            modifier = Modifier
                                .offset(y = topOffset)
                                .fillMaxWidth()
                                .height(cardHeight)
                                .padding(horizontal = cardPadding, vertical = cardPadding)
                                .applyIf(!course.hasConflict) {
                                    shareElementComposed(
                                        sharedContentState = rememberSharedContentState(key = "summary-course-to-detail-${course.asNoConflict.record.id}"),
                                        animatedVisibilityScope = LocalAnimatedContentScope.current
                                    )
                                }
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
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = 0.7f
                                    ),
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
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = 0.7f
                                    ),
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

private val OnlyMonthAndDayFormat = LocalDate.Format {
    monthNumber()
    char('-')
    dayOfMonth()
}

val cardHeight = 110.dp  // 每节课高度
val cardPadding = 2.dp  // 每节课之间的空隙调整为更小的值，使卡片更紧凑
val columnHeight = 12 * (cardHeight + cardPadding) // 课程表总高度
