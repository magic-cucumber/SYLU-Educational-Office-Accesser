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
import top.kagg886.util.getTimeByLessonNumber

class SummaryModel(
    private val syncState: MainRouteViewState,
    private val database: AppDatabase
) : ViewModel(), ContainerHost<SummaryState, SummarySideEffect> {
    override val container: Container<SummaryState, SummarySideEffect> = container(SummaryState.Loading) {
        if (syncState == MainRouteViewState.SyncProcess) {
            return@container
        }
        if (syncState is MainRouteViewState.SyncFailed) {
            reduce { SummaryState.Failed }
            return@container
        }

        val courseRecordDao = database.courseRecordDao()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        //计算今天是校历第几周
        val weekNumber = try {
            AppSyncMMKV.calender!!.calculateWeekNumber(today)
        } catch (e: IllegalStateException) {
            //如果是假期则报错
            reduce {
                SummaryState.FailedButSuccess(e.message!!)
            }
            return@container
        }

        //获取今天的课表计划
        val plan = courseRecordDao.getCoursesWithRecordInfoByDate(
            weekNumber = weekNumber,
            dayOfWeek = today.dayOfWeek.isoDayNumber,
        )

        //将课表计划和课表信息合并
        reduce {
            SummaryState.Success(
                plan.groupBy { it.course }.flatMap { (course, records) ->
                    records.map { record->
                        TodayClass(
                            name = course.name,
                            teacher = course.teacherName,
                            location = course.classroomName,
                            date = getTimeByLessonNumber(record.record.periodOfDay)
                        )
                    }
                }
            )
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
        val plan: List<TodayClass>,
    ) : SummaryState


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

}
