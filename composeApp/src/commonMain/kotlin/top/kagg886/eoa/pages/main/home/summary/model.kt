package top.kagg886.eoa.pages.main.home.summary

import androidx.lifecycle.ViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
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
            reduce { SummaryState.Syncing }
            return@container
        }
        if (syncState is MainRouteViewState.SyncFailed) {
            reduce { SummaryState.Failed }
            return@container
        }

        val courseRecordDao = database.courseRecordDao()
        val courseDao = database.courseDao()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val weekNumber = try {
            AppSyncMMKV.calender!!.calculateWeekNumber(today)
        } catch (e: IllegalStateException) {
            reduce {
                SummaryState.FailedButSuccess(e.message!!)
            }
            return@container
        }

        val plan = courseRecordDao.getTodayClassesByDateParam(
            weekNumber = weekNumber,
            dayOfWeek = today.dayOfWeek.isoDayNumber,
        )

        val courses = run {
            val haveCourseCode = plan.map { it.courseId }.toSet()
            courseDao.all().filter { it.id in haveCourseCode }
        }

        reduce {
            SummaryState.Success(
                courses.flatMap { course->
                    plan.filter { it.courseId == it.id }.map { record->
                        TodayClass(
                            name = course.name,
                            teacher = course.teacherName,
                            date = getTimeByLessonNumber(record.periodOfDay),
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
     * 同步中
     */
    data object Syncing : SummaryState

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
        val msg:String,
    ) : SummaryState
}

sealed interface SummarySideEffect {

}
