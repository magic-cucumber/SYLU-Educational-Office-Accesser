package top.kagg886.eoa.pages.main.home.summary

import androidx.lifecycle.ViewModel
import kotlinx.datetime.*
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseExtendEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getPeriodNumber
import top.kagg886.util.getTimeByLessonNumber
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SummaryModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), ContainerHost<SummaryState, SummarySideEffect> {
    private val courseRecordDao = database.courseRecordDao()
    private val courseExtendDao = database.courseExtendDao()

    override val container: Container<SummaryState, SummarySideEffect> =
        container(SummaryState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    SummaryState.Failed(syncState.message)
                }
                return@container
            }

            // 正在同步则展示加载中
            if (syncState is MainRouteViewState.SyncProcess) {
                // 如果有脏数据则展示
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则展示加载中
                reduce {
                    SummaryState.Loading
                }
                return@container
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                setDataUnsafe().join()
                return@container
            }
        }

    @OptIn(ExperimentalTime::class)
    fun setDataUnsafe() = intent {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val (isInHoliday, isBeforeInTerm, currentWeek) = AppSyncMMKV.calender!!.calculateWeekNumber()

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

        //获取今天的课表计划
        val plan = courseRecordDao.getCoursesWithRecordInfo(
            weekNumber = currentWeek,
            dayOfWeek = today.dayOfWeek.isoDayNumber,
        )

        //将课表计划和课表信息合并
        val period = today.time.getPeriodNumber()
        val progress = period?.let {
            val (start, end) = getTimeByLessonNumber(it)
            val all = end.toSecondOfDay() - start.toSecondOfDay()
            val current = today.time.toSecondOfDay() - start.toSecondOfDay()
            current.toFloat() / all.toFloat()
        }

        val extendClass = courseExtendDao.all(currentWeek)

        val plans: List<TodayClass> = plan
            .map { (course, record) ->
                TodayClass.Single(
                    name = course.name,
                    teacher = course.teacherName,
                    location = course.classroomName,
                    date = getTimeByLessonNumber(record.periodOfDay),
                    recordId = record.id!!,
                    courseId = course.id!!,
                    progress = if (period == record.periodOfDay) progress else null,
                    isDegreeProgram = course.isDegreeRequired,
                    isExamine = course.isExaminable,
                )
            }
            .groupBy { it.date }
            .map { (_, classes) ->
                if (classes.size == 1) {
                    classes.single()
                } else {
                    TodayClass.Conflict(
                        date = classes.first().date,
                        progress = classes.first().progress,
                        data = classes
                    )
                }
            }
        reduce {
            SummaryState.Success(
                weekNumber = currentWeek,
                dayPeriod = period,
                progress = with(AppSyncMMKV.calender!!) {
                    start.until(today.date, DateTimeUnit.DAY).toFloat() / start.until(end, DateTimeUnit.DAY)
                },
                plan = plans,
                extendClass = extendClass
            )
        }
    }

    fun redirectToCourse(it: TodayClass) = intent {
        when (it) {
            is TodayClass.Single -> {
                postSideEffect(SummarySideEffect.NavigateToCourseInfo(it.recordId))
            }

            is TodayClass.Conflict -> {
                val sample = courseRecordDao.getById(it.data.first().recordId)
                postSideEffect(
                    SummarySideEffect.NavigateToConflictInfo(
                        weekNumber = sample.weekNumber,
                        dayOfWeek = sample.dayOfWeek,
                        periodOfDay = sample.periodOfDay
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
        val courseId: Long,
    ) : SummarySideEffect

    data class NavigateToConflictInfo(
        val weekNumber: Int, val dayOfWeek: Int, val periodOfDay: Int
    ) : SummarySideEffect
}
