package top.kagg886.eoa.pages.main.home.course.list

import androidx.lifecycle.ViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.pages.main.MainRouteViewState

class CoursePageViewModel(
    private val syncState: MainRouteViewState,
    private val weekNumber: Int,
    database: AppDatabase
) : ViewModel(), ContainerHost<CoursePageState, CoursePageSideEffect> {

    private val courseRecordDao = database.courseRecordDao()

    override val container =
        container<CoursePageState, CoursePageSideEffect>(CoursePageState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    CoursePageState.Failed
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
                    CoursePageState.Loading
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
        val courseGroupByWeekNumber = courseRecordDao
            .getCoursesWithRecordInfoByDate(weekNumber = weekNumber)
            .groupBy { it.record.dayOfWeek }
            .map { (weekNumber, courseAndRecord) ->
                weekNumber to courseAndRecord.groupBy { it.record.periodOfDay }
            }
            .toMap()

        reduce {
            CoursePageState.Success(
                thisWeekStartDate = AppSyncMMKV.calender!!.start.plus(
                    weekNumber - 1,
                    DateTimeUnit.WEEK
                ),
                currentWeekCourse = courseGroupByWeekNumber,
                currentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            )
        }
    }
}


sealed interface CoursePageState {
    data object Loading : CoursePageState
    data object Failed : CoursePageState
    data class Success(
        val thisWeekStartDate: LocalDate,
        val currentDate: LocalDate,
        val currentWeekCourse: Map<Int, Map<Int, MaybeConflictCourse>> //key为星期几，值为 该天的所有课程
    ) : CoursePageState
}

sealed interface CoursePageSideEffect {
}