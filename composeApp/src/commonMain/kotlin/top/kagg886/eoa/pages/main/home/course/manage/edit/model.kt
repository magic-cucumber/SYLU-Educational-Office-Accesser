package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.eoa.util.SnackBarType

class CourseEditModel(
    database: AppDatabase,
    val courseId: Long?
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
                    courseInfo = courseInfo,
                    recordInfo = records,
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
            val id = courseDao.insert(state.courseInfo)
            postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Success, "修改成功"))
            reduce {
                state.copy(
                    courseInfo = state.courseInfo.copy(id = id),
                )
            }
        }
    }
}


sealed interface CourseEditState {
    data object Loading : CourseEditState
    data class Success(
        val courseInfo: CourseEntity,
        val recordInfo: List<CourseRecordEntity>
    ) : CourseEditState
}

sealed interface CourseEditSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : CourseEditSideEffect
}