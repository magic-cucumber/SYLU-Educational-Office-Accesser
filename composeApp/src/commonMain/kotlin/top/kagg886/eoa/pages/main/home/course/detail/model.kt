package top.kagg886.eoa.pages.main.home.course.detail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import top.kagg886.eoa.util.BaseViewModel
import kotlinx.datetime.*
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import kotlin.time.Clock

class CourseDetailViewModel(
    private val recordId: Long,
    private val syncState: MainRouteViewState,
    database: AppDatabase,
    private val showHolidayCourseFlow: MutableStateFlow<Boolean>,
) : BaseViewModel<CourseDetailState, CourseDetailSideEffect>(
    name = "CourseDetailViewModel",
    initial = CourseDetailState.Loading
) {
    private val courseRecordDao = database.courseRecordDao()
    private val courseDao = database.courseDao()

    override suspend fun Syntax<CourseDetailState, CourseDetailSideEffect>.init() {
        if (syncState is MainRouteViewState.SyncFailed) {
            // 非首次同步则展示脏数据
            if (syncState.haveDirtyData) {
                setDataUnsafe()
                return
            }
            // 否则提示同步失败
            reduce {
                CourseDetailState.Failed(syncState.message)
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
                CourseDetailState.Loading
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
        //因为配置了删除课程时级联删除记录，所以我们无需过滤xnm和xqm。
        val calendar = AppSyncMMKV.calender!!
        //获取课程实体
        val course = courseDao.getById(courseRecordDao.getById(recordId).courseId!!)

        courseRecordDao.getByCourseIdFlow(course.id!!)
            .flowOn(Dispatchers.IO)
            .combine(showHolidayCourseFlow) { course, showHolidayCourse -> course to showHolidayCourse }
            .collect { (plans, showHolidayCourse) ->
                //获取该课程在本学期的进度
                val progress = with(plans.indexOfFirst { it.id == recordId }) {
                    when {
                        this == -1 -> null
                        this == plans.size - 1 -> 1f
                        else -> this / (plans.size - 1f)
                    }
                }
                val current = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                reduce {
                    CourseDetailState.Success(
                        entity = course,
                        records = plans
                            //过滤法定节假日当天的所有课程
                            .filter { (it.startTime.date !in calendar.holidays) || showHolidayCourse }
                            .map {
                                CourseRecordAndProgress(
                                    id = it.id!!,
                                    courseId = it.courseId!!,
                                    isUserAdded = it.isUserAdded,
                                    progressStatus = when {
                                        current < it.startTime -> CourseRecordAndProgress.ProgressStatus.NotStarted
                                        current > it.endTime -> CourseRecordAndProgress.ProgressStatus.Completed
                                        else -> CourseRecordAndProgress.ProgressStatus.InProgress
                                    },
                                    start = it.startTime,
                                    end = it.endTime,
                                )
                            },
                        progress = progress,
                    )
                }
            }


    }
}

sealed interface CourseDetailState {
    data class Success(
        val entity: CourseEntity,
        val records: List<CourseRecordAndProgress>,
        val progress: Float?,
    ) : CourseDetailState

    data class Failed(val msg: String) : CourseDetailState
    data object Loading : CourseDetailState
}

sealed interface CourseDetailSideEffect {
    data class ShowToast(val message: String) : CourseDetailSideEffect
}
