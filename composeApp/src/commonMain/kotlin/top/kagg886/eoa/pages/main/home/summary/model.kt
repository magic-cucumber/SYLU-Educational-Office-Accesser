package top.kagg886.eoa.pages.main.home.summary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import top.kagg886.eoa.util.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.*
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseExtendEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getPeriodNumber
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class SummaryModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase,
    private val showHolidayCourseFlow: MutableStateFlow<Boolean>
) : BaseViewModel<SummaryState, SummarySideEffect>(
    name = "SummaryModel",
    initial = SummaryState.Loading
) {
    private val courseRecordDao = database.courseRecordDao()
    private val courseExtendDao = database.courseExtendDao()

    @OptIn(OrbitExperimental::class)
    override suspend fun Syntax<SummaryState, SummarySideEffect>.init() {
        initData()
        //进度监听。每分钟调度一次
        intent {
            while (true) {
                awaitRunOn<SummaryState.Success> {
                    logger.i("refresh summary UI")
                    val timeZone = TimeZone.currentSystemDefault()
                    val current = Clock.System.now().toEpochMilliseconds()
                    for (i in state.plan) {
                        val start = i.date.first.toInstant(timeZone).toEpochMilliseconds()
                        val end = i.date.second.toInstant(timeZone).toEpochMilliseconds()
                        val progress = current
                            .takeIf { it in start..<end }
                            ?.let { (it - start).toFloat() / (end - start).toFloat() }
                        val state = i.progress as MutableStateFlow<Float?>
                        state.emit(progress)

                        logger.d("summary UI refresh result: $i --> $progress")
                    }
                }
                delay(1.minutes)
                logger.i("prepare for refresh summary UI")
            }
        }

        //每日0:00刷新UI。
        intent {
            while (true) {
                val timeZone = TimeZone.currentSystemDefault()
                val now = Clock.System.now()

                val today = now.toLocalDateTime(timeZone).date
                val nextMidnight = today
                    .plus(1, DateTimeUnit.DAY)
                    .atStartOfDayIn(timeZone)

                delay(nextMidnight - now)

                reduce { SummaryState.Loading }
                initData()
            }
        }
    }

    private fun initData() = intent {
        if (syncState is MainRouteViewState.SyncFailed) {
            // 非首次同步则展示脏数据
            if (syncState.haveDirtyData) {
                setDataUnsafe()
                return@intent
            }
            // 否则提示同步失败
            reduce {
                SummaryState.Failed(syncState.message)
            }
            return@intent
        }

        // 正在同步则展示加载中
        if (syncState is MainRouteViewState.SyncProcess) {
            // 如果有脏数据则展示
            if (syncState.haveDirtyData) {
                setDataUnsafe()
                return@intent
            }
            // 否则展示加载中
            reduce {
                SummaryState.Loading
            }
            return@intent
        }

        // 同步成功则展示数据
        if (syncState is MainRouteViewState.SyncSuccess) {
            setDataUnsafe()
            return@intent
        }
    }

    @OptIn(ExperimentalTime::class)
    fun setDataUnsafe() = intent {
        val calendar = AppSyncMMKV.calender ?: return@intent
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val (isInHoliday, isBeforeInTerm, currentWeek) = calendar.calculateWeekNumber()

        if (currentWeek == -1) {
            when {
                isInHoliday -> reduce {
                    SummaryState.FailedButSuccess("享受假期吧！")
                }

                isBeforeInTerm -> reduce {
                    SummaryState.FailedButSuccess("准备开学吧！")
                }
            }
            return@intent
        }

        courseRecordDao
            .getCoursesWithRecordInfoFlow(
                start = today.date.atTime(0, 0),
                end = today.date.plus(1, DateTimeUnit.DAY).atTime(0, 0)
            )
            .flowOn(Dispatchers.IO)
            .combine(showHolidayCourseFlow) { course, showHolidayCourse -> course to showHolidayCourse }
            .collect { (plan, showHolidayCourse) ->
                //将课表计划和课表信息合并
                val period = today.time.getPeriodNumber()
                val extendClass = courseExtendDao.all(currentWeek)
                val timeZone = TimeZone.currentSystemDefault()
                val now = today.toInstant(timeZone).toEpochMilliseconds()

                val plans: List<TodayClass> = plan
                    //过滤法定节假日当天的所有课程
                    .filter { (it.record.startTime.date !in calendar.holidays) || showHolidayCourse }
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
                            fullDate = record.startTime to record.endTime,
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

                reduce {
                    SummaryState.Success(
                        weekNumber = currentWeek,
                        dayPeriod = period,
                        progress = with(AppSyncMMKV.calender!!) {
                            start.until(today.date, DateTimeUnit.DAY).toFloat() / start.until(
                                end,
                                DateTimeUnit.DAY
                            )
                        },
                        plan = plans,
                        extendClass = extendClass
                    )
                }
            }
    }

    fun redirectToCourse(it: TodayClass) = intent {
        when (it) {
            is TodayClass.Single -> {
                postSideEffect(
                    SummarySideEffect.NavigateToCourseInfo(
                        CourseDetailRoute(
                            recordId = it.recordId,
                            source = "summary",
                            startTime = it.date.first,
                            endTime = it.date.second,
                        )
                    )
                )
            }

            is TodayClass.Conflict -> {
                postSideEffect(
                    SummarySideEffect.NavigateToConflictInfo(
                        startTime = it.date.first,
                        endTime = it.date.second
                    )
                )
            }
        }
    }
}

sealed interface SummaryState {
    /**
     * 初始状态
     */
    data object Loading : SummaryState

    /**
     * 同步成功
     */
    data class Success(
        val weekNumber: Int,
        val dayPeriod: Int?,
        val progress: Float,
        val extendClass: List<CourseExtendEntity>,
        val plan: List<TodayClass>,
    ) : SummaryState


    /**
     * 同步失败
     */
    data class Failed(val msg: String) : SummaryState

    /**
     * 同步成功，但是有情况导致无法显示课表
     * 例如正在放假
     */
    data class FailedButSuccess(
        val msg: String,
    ) : SummaryState
}

sealed interface SummarySideEffect {
    data class NavigateToCourseInfo(
        val route: CourseDetailRoute,
    ) : SummarySideEffect

    data class NavigateToConflictInfo(val startTime: LocalDateTime, val endTime: LocalDateTime) :
        SummarySideEffect
}

private enum class EventType {
    START, END
}

private data class SweepState(
    val time: LocalDateTime? = null,
    val active: Set<TodayClass.Single> = emptySet(),
)
