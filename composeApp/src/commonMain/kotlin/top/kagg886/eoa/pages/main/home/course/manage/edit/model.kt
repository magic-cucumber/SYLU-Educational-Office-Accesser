package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppAiMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.eoa.util.SnackBarType
import kotlin.time.Duration.Companion.seconds

class CourseEditModel(
    database: AppDatabase,
    courseId: Long?
) : ViewModel(), ContainerHost<CourseEditState, CourseEditSideEffect> {

    private val courseDao = database.courseDao()
    private val courseRecordDao = database.courseRecordDao()
    override val container =
        container<CourseEditState, CourseEditSideEffect>(CourseEditState.Loading) {
            val (xnm, xqm) = AppSyncMMKV.picker!!.default.asTerm()
            val courseInfo = courseId?.let { courseDao.getById(it) } ?: CourseEntity(
                name = "",
                teacherName = "",
                classroomName = "",
                credits = 0f,
                isDegreeRequired = false,
                yearCode = xnm,
                semesterCode = xqm,
                isUserAdded = true
            )

            val records = courseId?.let { courseRecordDao.getByCourseId(it) } ?: emptyList()

            reduce {
                CourseEditState.Success(
                    courseId = courseId,
                    courseInfo = courseInfo,
                    recordInfo = records,
                    startDate = AppSyncMMKV.calender!!.start,
                    allWeekNumber = AppSyncMMKV.calender!!.count(),
                    enableSaveButton = true,
                    aiKey = AppAiMMKV.apiKey,
                    aiEndpoint = AppAiMMKV.endpoint,
                    aiModel = AppAiMMKV.model
                )
            }
        }


    @OptIn(OrbitExperimental::class)
    fun modifyCourse(it: CourseEntity) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    courseInfo = it,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun confirmModifyCourse() = intent {
        runOn<CourseEditState.Success> {
            if (state.courseInfo.name.isBlank()) {
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "课程名不能为空"))
                return@runOn
            }
            if (state.courseInfo.classroomName.isBlank()) {
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "教室不能为空"))
                return@runOn
            }
            if (state.recordInfo.isEmpty()) {
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "请添加课程时间"))
                return@runOn
            }
            reduce { state.copy(enableSaveButton = false) } //防止重复点击
            val id = state.courseId?.apply { courseDao.update(state.courseInfo) } ?: courseDao.insert(state.courseInfo)
            if (state.courseId == null) {
                courseRecordDao.insertAll(state.recordInfo.map { it.copy(courseId = id) })
            }
            postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Success, "修改成功"))
            reduce {
                state.copy(
                    courseInfo = state.courseInfo.copy(id = id),
                )
            }
            delay(3.seconds)
            postSideEffect(CourseEditSideEffect.NavigateBack)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun addRecord(weekNumber: Int, dayOfWeek: Int, periodOfDay: Int) = intent {
        runOn<CourseEditState.Success> {
            val record = CourseRecordEntity(
                id = null,
                courseId = state.courseId,
                weekNumber = weekNumber,
                dayOfWeek = dayOfWeek,
                periodOfDay = periodOfDay,
                isUserAdded = true
            )
            //是修改模式则编辑数据库
            if (state.courseId != null) {
                courseRecordDao.insert(record)
            }
            reduce {
                state.copy(
                    recordInfo = state.recordInfo + record
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun deleteRecord(it: CourseRecordEntity) = intent {
        runOn<CourseEditState.Success> {
            //是修改模式则编辑数据库
            if (it.courseId != null) {
                courseRecordDao.delete(it)
            }
            reduce {
                state.copy(
                    recordInfo = state.recordInfo - it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setAiEndpoint(it: String) = intent {
        runOn<CourseEditState.Success> {
            AppAiMMKV.endpoint = it
            reduce {
                state.copy(
                    aiEndpoint = it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setAiKey(it: String) = intent {
        runOn<CourseEditState.Success> {
            AppAiMMKV.apiKey = it
            reduce {
                state.copy(
                    aiKey = it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setAiModel(it: String) = intent {
        runOn<CourseEditState.Success> {
            AppAiMMKV.model = it
            reduce {
                state.copy(
                    aiModel = it
                )
            }
        }
    }

    fun generateCourseByAI(it: String) = intent {
        postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Info, "敬请期待"))
    }
}


sealed interface CourseEditState {
    data object Loading : CourseEditState
    data class Success(
        val courseId: Long?,
        val enableSaveButton: Boolean,
        val courseInfo: CourseEntity,
        val recordInfo: List<CourseRecordEntity>,
        val allWeekNumber: Int,
        val startDate: LocalDate,

        val aiKey: String,
        val aiEndpoint: String,
        val aiModel: String
    ) : CourseEditState
}

sealed interface CourseEditSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : CourseEditSideEffect
    data object NavigateBack : CourseEditSideEffect
}
