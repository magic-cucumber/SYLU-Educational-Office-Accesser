package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.calculateWeekNumber
import kotlin.time.Clock

class CourseListViewModel(
    private val syncState: MainRouteViewState,
) : ViewModel(), OrbitContainerHost<CourseListState, CourseListState, CourseListSideEffect> {
    private val logger = "CourseListViewModel".asTaggedLogger

    override val container =
        orbitContainer<CourseListState, CourseListSideEffect>(CourseListState.Loading) {
            refresh().join()

            //每日0:00刷新UI。立即执行
            intent {
                while (true) {
                    val timeZone = TimeZone.currentSystemDefault()
                    val now = Clock.System.now()

                    val today = now.toLocalDateTime(timeZone).date
                    val nextMidnight = today
                        .plus(1, DateTimeUnit.DAY)
                        .atStartOfDayIn(timeZone)

                    logger.i("we will delay ${nextMidnight - now} to refresh course UI")

                    delay(nextMidnight - now)

                    reduce { CourseListState.Loading }
                    refresh().join()
                }
            }
        }

    fun setDataUnsafe() = intent {
        val (isInHoliday, isBeforeInTerm, currentWeek) = AppSyncMMKV.calender!!.calculateWeekNumber()

        if (currentWeek == -1) {
            when {
                isInHoliday -> reduce {
                    CourseListState.FailedButSuccess("享受假期吧！")
                }

                isBeforeInTerm -> reduce {
                    CourseListState.FailedButSuccess("准备开学吧！")
                }
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

    fun selectToWeek(data: Int? = null) = intent {
        val s = state
        if (s !is CourseListState.Success) {
            postSideEffect(CourseListSideEffect.Toast("正在加载中，请稍等片刻"))
            return@intent
        }
        val week = data ?: (s.currentWeek - 1)
        if (week == s.state.currentPage) {
            postSideEffect(CourseListSideEffect.Toast("当前周数无需跳转"))
            return@intent
        }
        // 只发送事件，不直接处理动画
        postSideEffect(CourseListSideEffect.ScrollToCurrentWeek(week))
    }

    fun refresh() = intent {
        if (syncState is MainRouteViewState.SyncFailed) {
            // 非首次同步则展示脏数据
            if (syncState.haveDirtyData) {
                setDataUnsafe().join()
                return@intent
            }
            // 否则提示同步失败
            reduce {
                CourseListState.Failed(syncState.message)
            }
            return@intent
        }

        // 正在同步则展示加载中
        if (syncState is MainRouteViewState.SyncProcess) {
            // 如果有脏数据则展示
            if (syncState.haveDirtyData) {
                setDataUnsafe().join()
                return@intent
            }
            // 否则展示加载中
            reduce {
                CourseListState.Loading
            }
            return@intent
        }

        // 同步成功则展示数据
        if (syncState is MainRouteViewState.SyncSuccess) {
            setDataUnsafe().join()
            return@intent
        }
    }
}


sealed interface CourseListState {
    data object Loading : CourseListState
    data class Failed(val msg: String) : CourseListState

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
    data class Toast(val msg: String) : CourseListSideEffect
    data class ScrollToCurrentWeek(val page: Int) : CourseListSideEffect
}
