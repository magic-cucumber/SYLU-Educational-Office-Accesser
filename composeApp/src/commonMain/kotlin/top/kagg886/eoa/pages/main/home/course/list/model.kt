package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.foundation.pager.PagerState
import top.kagg886.eoa.util.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.util.calculateWeekNumber
import kotlin.time.Clock
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

class CourseListViewModel(
    private val syncState: MainRouteViewState,
    private val database: AppDatabase,
    private val minuteHeightPx: Float,
    private val showHolidayCourseFlow: MutableStateFlow<Boolean>,
) : BaseViewModel<CourseListState, CourseListSideEffect>(
    name = "CourseListViewModel",
    initial = CourseListState.Loading
) {
    override suspend fun Syntax<CourseListState, CourseListSideEffect>.init() {
        refresh().join()

        //每日0:00刷新UI。立即执行
        intent {
            while (true) {
                val timeZone = TimeZone.currentSystemDefault()
                val now = Clock.System.now()

                val today = now.toLocalDateTime(timeZone).date
                val nextMidnight = today
                    .plus(1, DateTimeUnit.DAY)
                    .atStartOfDayIn(timeZone)

                logger.i("we will delay ${nextMidnight - now} to refresh course UI")

                delay(nextMidnight - now)

                reduce { CourseListState.Loading }
                refresh().join()
            }
        }
    }

    fun setDataUnsafe() = intent {
        val (isInHoliday, isBeforeInTerm, currentWeek) = AppSyncMMKV.calender!!.calculateWeekNumber()
        val allWeek = AppSyncMMKV.calender!!.count()
        val scrollOffset = if (isBeforeInTerm || currentWeek != -1) {
            initialScrollOffset(if (isBeforeInTerm) 0 else currentWeek - 1)
        } else {
            0
        }
        if (currentWeek == -1) {
            when {
                isInHoliday -> reduce {
                    CourseListState.AfterTerm
                }

                isBeforeInTerm -> reduce {
                    CourseListState.BeforeTerm(
                        state = PagerState(currentPage = 0) { allWeek },
                        allWeek = allWeek,
                        scale = MutableStateFlow(1f),
                        scrollOffset = MutableStateFlow(scrollOffset),
                    )
                }
            }
            return@intent
        }
        reduce {
            CourseListState.Success(
                currentWeek = currentWeek,
                allWeek = allWeek,
                //currentPage是index，从0开始
                state = PagerState(currentPage = currentWeek - 1) { allWeek },
                scale = MutableStateFlow(1f),
                scrollOffset = MutableStateFlow(scrollOffset),
            )
        }
    }

    private suspend fun initialScrollOffset(page: Int): Int {
        val calendar = AppSyncMMKV.calender!!
        val weekStart = calendar.start.plus(page, DateTimeUnit.WEEK).atTime(0, 0)
        val weekEnd = calendar.start.plus(page + 1, DateTimeUnit.WEEK).atTime(0, 0)
        val courses = database.courseRecordDao()
            .getCoursesWithRecordInfoFlow(weekStart, weekEnd)
            .first()
            .filter { it.record.startTime.date !in calendar.holidays || showHolidayCourseFlow.value }
        val range = timelineRangeOf(courses.map { it.record.startTime.time to it.record.endTime.time })
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        val elapsedMinutes = (
            (currentTime.toSecondOfDay() - 20.minutes.inWholeSeconds - range.start.toSecondOfDay()) / 60f
        ).coerceIn(0f, range.durationMinutes.toFloat())
        return (minuteHeightPx * elapsedMinutes).roundToInt()
    }

    fun selectToWeek(data: Int? = null) = intent {
        val s = state
        if (s !is CourseListState.DataAccessible) {
            postSideEffect(CourseListSideEffect.Toast("正在加载中，请稍等片刻"))
            return@intent
        }
        val week = data ?: (((s as? CourseListState.Success)?.currentWeek ?: 1) - 1)
        if (week == s.state.currentPage) {
            postSideEffect(CourseListSideEffect.Toast("当前周数无需跳转"))
            return@intent
        }
        // 只发送事件，不直接处理动画
        postSideEffect(CourseListSideEffect.ScrollToCurrentWeek(week))
    }

    fun scaleBy(zoomChange: Float) = intent {
        runOn<CourseListState.DataAccessible> {
            state.scale.value = (state.scale.value * zoomChange)
                .coerceIn(0.5f, 2f)
        }
    }

    fun setScrollOffset(offset: Int) = intent {
        runOn<CourseListState.DataAccessible> {
            state.scrollOffset.value = offset
        }
    }

    fun refresh() = intent {
        if (syncState is MainRouteViewState.SyncFailed) {
            // 非首次同步则展示脏数据
            if (syncState.haveDirtyData) {
                setDataUnsafe().join()
                return@intent
            }
            // 否则提示同步失败
            reduce {
                CourseListState.Failed(syncState.message)
            }
            return@intent
        }

        // 正在同步则展示加载中
        if (syncState is MainRouteViewState.SyncProcess) {
            // 如果有脏数据则展示
            if (syncState.haveDirtyData) {
                setDataUnsafe().join()
                return@intent
            }
            // 否则展示加载中
            reduce {
                CourseListState.Loading
            }
            return@intent
        }

        // 同步成功则展示数据
        if (syncState is MainRouteViewState.SyncSuccess) {
            setDataUnsafe().join()
            return@intent
        }
    }
}


sealed interface CourseListState {
    data object Loading : CourseListState
    data class Failed(val msg: String) : CourseListState

    data class Success(
        override val state: PagerState,
        val currentWeek: Int, //当前周数
        override val allWeek: Int, //总周数
        override val scale: MutableStateFlow<Float>, //缩放倍数
        override val scrollOffset: MutableStateFlow<Int>,
    ) : DataAccessible

    data object AfterTerm : CourseListState
    data class BeforeTerm(
        override val state: PagerState,
        override val allWeek: Int,
        override val scale: MutableStateFlow<Float>,
        override val scrollOffset: MutableStateFlow<Int>,
    ) : DataAccessible

    interface DataAccessible : CourseListState {
        val state: PagerState
        val allWeek: Int

        val scale: MutableStateFlow<Float>
        val scrollOffset: MutableStateFlow<Int>
    }
}

internal fun timelineRangeOf(courseTimes: List<Pair<LocalTime, LocalTime>>): TimelineRange {
    val defaultStart = LocalTime(8, 0)
    val defaultEnd = LocalTime(22, 0)
    return TimelineRange(
        start = minOf(defaultStart, courseTimes.minOfOrNull { it.first } ?: defaultStart),
        end = maxOf(defaultEnd, courseTimes.maxOfOrNull { it.second } ?: defaultEnd),
    )
}

internal data class TimelineRange(
    val start: LocalTime,
    val end: LocalTime,
) {
    val durationMinutes: Int
        get() = (end.toSecondOfDay() - start.toSecondOfDay()) / 60

    fun marks(stepMinutes: Int): IntProgression =
        (start.toSecondOfDay() / 60) until (end.toSecondOfDay() / 60) step stepMinutes
}

sealed interface CourseListSideEffect {
    data class Toast(val msg: String) : CourseListSideEffect
    data class ScrollToCurrentWeek(val page: Int) : CourseListSideEffect
}
