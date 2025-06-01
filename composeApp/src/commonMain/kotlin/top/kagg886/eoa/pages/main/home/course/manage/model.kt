package top.kagg886.eoa.pages.main.home.course.manage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.util.calculateWeekNumber

@Composable
fun courseManageModel(
    syncState: MainRouteViewState,
): CourseManageModel {
    val nav = LocalNavController.current
    val entry = remember {
        nav.getBackStackEntry(CourseManageRoute) // 嵌套图 route
    }
    val mainModel = mainViewModel()
    val model = viewModel(
        viewModelStoreOwner = entry,
        key = syncState.toString(),
        initializer = {
            CourseManageModel(syncState, mainModel.database)
        }
    )

    return model
}

class CourseManageModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), ContainerHost<CourseManageState, CourseManageSideEffect> {
    private val courseDao = database.courseDao()
    override val container: Container<CourseManageState, CourseManageSideEffect> =
        container(CourseManageState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    CourseManageState.Failed
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
                    CourseManageState.Loading
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
        val currentWeek = try { //从1开始
            AppSyncMMKV.calender!!.calculateWeekNumber(
                Clock.System.todayIn(TimeZone.currentSystemDefault())
            )
        } catch (e: Exception) {
            reduce {
                CourseManageState.FailedButSuccess(
                    msg = e.message ?: "未知错误"
                )
            }
            return@intent
        }
        val entity = courseDao.all()
        reduce {
            CourseManageState.Success(
                currentWeek = currentWeek,
                data = entity,
                onlyShowUserCourse = false,
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun toggleSystemCourseVisible() = intent {
        runOn<CourseManageState.Success> {
            val data = courseDao.all(onlyUserAdded = !state.onlyShowUserCourse)
            reduce {
                state.copy(
                    onlyShowUserCourse = !state.onlyShowUserCourse,
                    data = data,
                )
            }
        }
    }
}


sealed interface CourseManageState {
    data object Loading : CourseManageState
    data object Failed : CourseManageState

    data class Success(
        val currentWeek: Int,
        val onlyShowUserCourse: Boolean,
        val data: List<CourseEntity>
    ) : CourseManageState

    data class FailedButSuccess(
        val msg: String,
    ) : CourseManageState
}

sealed interface CourseManageSideEffect {

}