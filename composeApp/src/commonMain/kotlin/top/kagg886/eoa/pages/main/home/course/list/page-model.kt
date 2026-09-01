package top.kagg886.eoa.pages.main.home.course.list

import top.kagg886.eoa.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.*
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.summary.TodayClass
import kotlin.time.Clock

class CoursePageViewModel(
    private val syncState: MainRouteViewState,
    private val weekIndex: Int,
    database: AppDatabase
) : BaseViewModel<CoursePageState, CoursePageSideEffect>(
    name = "CoursePageViewModel",
    initial = CoursePageState.Loading
) {

    private val courseRecordDao = database.courseRecordDao()

    override suspend fun Syntax<CoursePageState, CoursePageSideEffect>.init() {
        if (syncState is MainRouteViewState.SyncFailed) {
            // 非首次同步则展示脏数据
            if (syncState.haveDirtyData) {
                setDataUnsafe()
                return
            }
            // 否则提示同步失败
            reduce {
                CoursePageState.Failed(syncState.message)
            }
            return
        }

        // 正在同步则展示加载中
        if (syncState is MainRouteViewState.SyncProcess) {
            // 如果有脏数据则展示
            if (syncState.haveDirtyData) {
                setDataUnsafe()
                return
            }
            // 否则展示加载中
            reduce {
                CoursePageState.Loading
            }
            return
        }

        // 同步成功则展示数据
        if (syncState is MainRouteViewState.SyncSuccess) {
            setDataUnsafe()
            return
        }
    }

    fun setDataUnsafe() = intent {
        val weekStartDate = AppSyncMMKV.calender!!.start.plus(weekIndex - 1, DateTimeUnit.WEEK).atTime(0,0)
        val weekEndDate = AppSyncMMKV.calender!!.start.plus(weekIndex, DateTimeUnit.WEEK).atTime(0,0)
        courseRecordDao
            .getCoursesWithRecordInfoFlow(weekStartDate,weekEndDate)
            .flowOn(Dispatchers.IO)
            .collect { course ->
                val timeZone = TimeZone.currentSystemDefault()
                val now = Clock.System.now().toEpochMilliseconds()
                val data = course
                    .groupBy { it.record.startTime.date.dayOfWeek.isoDayNumber }
                    .mapValues { (_, courseAndRecord) ->
                        courseAndRecord
                            .map { (course, record) ->
                                TodayClass.Single(
                                    name = course.name,
                                    teacher = course.teacherName,
                                    location = course.classroomName,
                                    date = record.startTime to record.endTime,
                                    recordId = record.id!!,
                                    courseId = course.id!!,
                                    isDegreeProgram = course.isDegreeRequired,
                                    isExamine = course.isExaminable,
                                )
                            }
                            .flatMap { course ->
                                listOf(
                                    course.date.first to (EventType.START to course),
                                    course.date.second to (EventType.END to course),
                                )
                            }
                            .groupBy(
                                keySelector = { it.first },
                                valueTransform = { it.second },
                            )
                            .entries
                            .sortedBy { it.key }
                            .asSequence()
                            .runningFold(SweepState()) { state, (time, events) ->
                                SweepState(
                                    time = time,
                                    active = buildSet {
                                        addAll(state.active)

                                        events
                                            .asSequence()
                                            .filter { it.first == EventType.END }
                                            .map { it.second }
                                            .forEach(::remove)

                                        events
                                            .asSequence()
                                            .filter { it.first == EventType.START }
                                            .map { it.second }
                                            .forEach(::add)
                                    },
                                )
                            }
                            .zipWithNext()
                            .mapNotNull { (previous, current) ->
                                val start = previous.time ?: return@mapNotNull null
                                val end = current.time ?: return@mapNotNull null

                                previous.active
                                    .takeIf { it.isNotEmpty() && start < end }
                                    ?.let { active ->
                                        val startMillis = start.toInstant(timeZone).toEpochMilliseconds()
                                        val endMillis = end.toInstant(timeZone).toEpochMilliseconds()
                                        val progress = now
                                            .takeIf { it in startMillis..<endMillis }
                                            ?.let {
                                                (it - startMillis).toFloat() /
                                                        (endMillis - startMillis).toFloat()
                                            }

                                        if (active.size == 1) {
                                            active.single().copy(
                                                date = start to end,
                                                progress = MutableStateFlow(progress),
                                            )
                                        } else {
                                            TodayClass.Conflict(
                                                date = start to end,
                                                progress = MutableStateFlow(progress),
                                                data = active.toList(),
                                            )
                                        }
                                    }
                            }
                            .toList()
                    }

                reduce {
                    CoursePageState.Success(
                        thisWeekStartDate = AppSyncMMKV.calender!!.start.plus(
                            weekIndex - 1,
                            DateTimeUnit.WEEK
                        ),
                        currentWeekCourse = data,
                        currentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    )
                }
            }
    }

    fun navigateToCourseDetail(it: TodayClass.Single) = intent {
        postSideEffect(
            CoursePageSideEffect.NavigateToCourseDetail(
                CourseDetailRoute(
                    recordId = it.recordId,
                    source = "list",
                    startTime = it.date.first,
                    endTime = it.date.second,
                )
            )
        )
    }

    fun navigateToConflictDetail(startTime: LocalDateTime,endTime: LocalDateTime) = intent {
        postSideEffect(
            CoursePageSideEffect.NavigateToConflictDetail(startTime,endTime)
        )
    }
}


sealed interface CoursePageState {
    data object Loading : CoursePageState
    data class Failed(val msg: String) : CoursePageState
    data class Success(
        val thisWeekStartDate: LocalDate,
        val currentDate: LocalDate,
        val currentWeekCourse: Map<Int, List<TodayClass>> //key为星期几，值为 该天的所有课程
    ) : CoursePageState
}

sealed interface CoursePageSideEffect {
    data class NavigateToCourseDetail(val route: CourseDetailRoute) : CoursePageSideEffect
    data class NavigateToConflictDetail(val startTime: LocalDateTime, val endTime: LocalDateTime) :
        CoursePageSideEffect
}

private enum class EventType {
    START, END
}

private data class SweepState(
    val time: LocalDateTime? = null,
    val active: Set<TodayClass.Single> = emptySet(),
)
