package top.kagg886.eoa.pages.main.home.course.manage.list

import androidx.lifecycle.ViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.util.calculateWeekNumber
import kotlin.time.Clock

class CourseManageListModel(
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
                    CourseManageState.Failed(syncState.message)
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

    fun setDataUnsafe(onlyShowUserCourse: Boolean = false) = intent {
        val (isInHoliday, isBeforeInTerm, currentWeek) = AppSyncMMKV.calender!!.calculateWeekNumber()

        var w = currentWeek
        if (isBeforeInTerm) w = 0
        if (isInHoliday) w = AppSyncMMKV.calender!!.count()

        val entity = courseDao.all()
        reduce {
            CourseManageState.Success(
                currentWeek = w,
                data = entity,
                onlyShowUserCourse = onlyShowUserCourse,
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

    fun openAddOrEditCourse(data: CourseEntity?) = intent {
        if (Clock.System.todayIn(TimeZone.currentSystemDefault()) !in with(AppSyncMMKV.calender!!) { start..end }) {
            postSideEffect(CourseManageSideEffect.Toast("当前未处于学期中，不可编辑/新建课程"))
            return@intent
        }

        if (data?.isUserAdded == false) {
            postSideEffect(CourseManageSideEffect.Toast("系统课程不可修改"))
            return@intent
        }
        postSideEffect(CourseManageSideEffect.NavigateToEditOrAdd(data?.id))
    }

    @OptIn(OrbitExperimental::class)
    fun deleteCourse(it: CourseEntity) = intent {
        runOn<CourseManageState.Success> {
            if (!it.isUserAdded) {
                postSideEffect(CourseManageSideEffect.Toast("系统课程不可删除"))
                return@runOn
            }
            courseDao.delete(it)
            postSideEffect(CourseManageSideEffect.Toast("删除成功"))
            setDataUnsafe(state.onlyShowUserCourse).join()
        }
    }

    fun startExportICS() = intent {
        when (val s = state) {
            is CourseManageState.Success -> {
                postSideEffect(CourseManageSideEffect.StartExportIcs(s.data))
            }

            else -> {
                postSideEffect(CourseManageSideEffect.Toast("此时不允许数据导出，请稍后再试"))
            }
        }
    }

    fun startExportCalender() = intent {
        when (val s = state) {
            is CourseManageState.Success -> {
                postSideEffect(CourseManageSideEffect.StartExportCalender(s.data))
            }

            else -> {
                postSideEffect(CourseManageSideEffect.Toast("此时不允许数据导出，请稍后再试"))
            }
        }
    }
}


sealed interface CourseManageState {
    data object Loading : CourseManageState
    data class Failed(val msg: String) : CourseManageState

    data class Success(
        val currentWeek: Int,
        val onlyShowUserCourse: Boolean,
        val data: List<CourseEntity>
    ) : CourseManageState
}

sealed interface CourseManageSideEffect {
    data class Toast(val msg: String) : CourseManageSideEffect
    data class NavigateToEditOrAdd(val courseId: Long?) : CourseManageSideEffect
    data class StartExportIcs(val course: List<CourseEntity>) : CourseManageSideEffect

    data class StartExportCalender(val course: List<CourseEntity>) : CourseManageSideEffect
}
