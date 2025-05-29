package top.kagg886.eoa.pages.main.home.summary

import androidx.lifecycle.ViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.util.calculateWeekNumber
import top.kagg886.util.getPeriodNumber
import top.kagg886.util.getTimeByLessonNumber
import kotlin.collections.component1
import kotlin.collections.component2

class SummaryModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), ContainerHost<SummaryState, SummarySideEffect> {
    private val courseRecordDao = database.courseRecordDao()

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
                    SummaryState.Failed
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

    fun setDataUnsafe() = intent {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        //计算今天是校历第几周
        val weekNumber = try {
            AppSyncMMKV.calender!!.calculateWeekNumber(today.date)
        } catch (e: IllegalStateException) {
            //如果是假期则报错
            reduce {
                SummaryState.FailedButSuccess(e.message!!)
            }
            return@intent
        }

        //获取今天的课表计划
        val plan = courseRecordDao.getCoursesWithRecordInfoByDate(
            weekNumber = weekNumber,
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
        reduce {
            SummaryState.Success(
                weekNumber = weekNumber,
                dayPeriod = period,
                progress = progress,
                plan = plan.groupBy { it.course }.flatMap { (course, records) ->
                    records.map { record ->
                        TodayClass(
                            name = course.name,
                            teacher = course.teacherName,
                            location = course.classroomName,
                            date = getTimeByLessonNumber(record.record.periodOfDay),
                            recordId = record.record.id!!,
                            courseId = course.id!!,
                            progress = if (period == record.record.periodOfDay) progress else null
                        )
                    }
                }
            )
        }
    }

    fun redirectToCourse(it: TodayClass) = intent {
        postSideEffect(SummarySideEffect.NavigateToCourseInfo(it.recordId))
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
        val dayPeriod:Int?,
        val progress: Float?,
        val plan: List<TodayClass>,
    ) : SummaryState


    /**
     * 同步失败
     */
    data object Failed : SummaryState

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
}
