package top.kagg886.eoa.pages.main.home.course.detail

import androidx.lifecycle.ViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.home.summary.SummaryState
import top.kagg886.util.calculateWeekNumber

class CourseDetailViewModel(
    private val recordId: Long,
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), ContainerHost<CourseDetailState, CourseDetailSideEffect> {
    private val courseRecordDao = database.courseRecordDao()
    private val courseDao = database.courseDao()


    override val container: Container<CourseDetailState, CourseDetailSideEffect> =
        container(CourseDetailState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    CourseDetailState.Failed
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
                    CourseDetailState.Loading
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
        //因为配置了删除课程时级联删除记录，所以我们无需过滤xnm和xqm。

        //获取课程实体
        val course = courseDao.getById(courseRecordDao.getById(recordId).courseId)

        //获取上课记录
        val plans = courseRecordDao.getByCourseId(course.id!!).sortedWith { a, b ->
            when {
                a.weekNumber != b.weekNumber -> a.weekNumber.compareTo(b.weekNumber)
                a.dayOfWeek != b.dayOfWeek -> a.dayOfWeek.compareTo(b.dayOfWeek)
                a.periodOfDay != b.periodOfDay -> a.periodOfDay.compareTo(b.periodOfDay)
                else -> 0
            }
        }

        //获取该课程在本学期的进度
        val progress = with(plans.indexOfFirst { it.id == recordId }) {
            when {
                this == -1 -> null
                this == plans.size - 1 -> 1f
                else -> this / (plans.size - 1f)
            }
        }

        reduce {
            CourseDetailState.Success(
                entity = course,
                records = plans,
                progress = progress,
            )
        }
    }
}

sealed interface CourseDetailState {
    data class Success(
        val entity: CourseEntity,
        val records: List<CourseRecordEntity>,
        val progress: Float?,
    ) : CourseDetailState

    data object Failed : CourseDetailState
    data object Loading : CourseDetailState

    data class FailedButSuccess(
        val msg: String,
    ) : CourseDetailState
}

sealed interface CourseDetailSideEffect {
    data class ShowToast(val message: String) : CourseDetailSideEffect
}