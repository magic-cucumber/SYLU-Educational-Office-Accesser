package top.kagg886.eoa.pages.main.home.course.list

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.pages.main.MainRouteViewState

class CoursePageViewModel(
    private val syncState: MainRouteViewState,
    weekNumber:Int,
    database: AppDatabase
): ViewModel(), ContainerHost<CoursePageState, CoursePageSideEffect> {
    override val container = container<CoursePageState, CoursePageSideEffect>(CoursePageState.Loading) {
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
        //TODO 从数据库拉取本周课程，然后设置单页课表数据
    }
}


sealed interface CoursePageState {
    data object Loading : CoursePageState
    data object Failed : CoursePageState
    data class Success(
        val currentWeekCourse: List<CourseEntity>
    ) : CoursePageState
}

sealed interface CoursePageSideEffect {
}