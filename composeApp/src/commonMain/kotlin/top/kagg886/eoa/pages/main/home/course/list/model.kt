package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.ViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.util.calculateWeekNumber

class CourseListViewModel(
    private val syncState: MainRouteViewState,
) : ViewModel(), ContainerHost<CourseListState, CourseListSideEffect> {

    override val container =
        container<CourseListState, CourseListSideEffect>(CourseListState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    CourseListState.Failed
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
                    CourseListState.Loading
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
                CourseListState.FailedButSuccess(
                    msg = e.message ?: "未知错误"
                )
            }
            return@intent
        }
        val allWeek = AppSyncMMKV.calender!!.count()
        reduce {
            CourseListState.Success(
                currentWeek = currentWeek,
                allWeek = allWeek,
                //currentPage是index，从0开始
                state = PagerState(currentPage = currentWeek - 1) { allWeek },
            )
        }
    }
}


sealed interface CourseListState {
    data object Loading : CourseListState
    data object Failed : CourseListState

    data class Success(
        val state: PagerState,
        val currentWeek: Int, //当前周数
        val allWeek: Int, //总周数
    ) : CourseListState

    data class FailedButSuccess(
        val msg: String,
    ) : CourseListState
}

sealed interface CourseListSideEffect {

}