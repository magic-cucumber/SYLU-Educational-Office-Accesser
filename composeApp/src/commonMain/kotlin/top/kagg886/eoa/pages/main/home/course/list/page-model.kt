package top.kagg886.eoa.pages.main.home.course.list

import top.kagg886.eoa.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.*
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.eoa.pages.main.MainRouteViewState
import kotlin.time.Clock

class CoursePageViewModel(
    private val syncState: MainRouteViewState,
    private val weekNumber: Int,
    database: AppDatabase
) : BaseViewModel<CoursePageState, CoursePageSideEffect>(name = "CoursePageViewModel", initial = CoursePageState.Loading) {

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
        courseRecordDao
            .getCoursesWithRecordInfoFlow(weekNumber = weekNumber)
            .flowOn(Dispatchers.IO)
            .collect { course ->
                val data = course
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
                        currentWeekCourse = data,
                        currentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    )
                }
            }
    }

    fun navigateToCourseDetail(it: CourseAndRecord) = intent {
        postSideEffect(CoursePageSideEffect.NavigateToCourseDetail(it.record.id!!))
    }

    fun navigateToConflictDetail(weekNumber: Int, dayOfWeek: Int, periodOfDay: Int) = intent {
        postSideEffect(CoursePageSideEffect.NavigateToConflictDetail(weekNumber, dayOfWeek, periodOfDay))
    }
}


sealed interface CoursePageState {
    data object Loading : CoursePageState
    data class Failed(val msg: String) : CoursePageState
    data class Success(
        val thisWeekStartDate: LocalDate,
        val currentDate: LocalDate,
        val currentWeekCourse: Map<Int, Map<Int, MaybeConflictCourse>> //key为星期几，值为 该天的所有课程
    ) : CoursePageState
}

sealed interface CoursePageSideEffect {
    data class NavigateToCourseDetail(val recordId: Long) : CoursePageSideEffect
    data class NavigateToConflictDetail(val weekNumber: Int, val dayOfWeek: Int, val periodOfDay: Int) :
        CoursePageSideEffect
}
