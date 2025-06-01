package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity

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
}


sealed interface CourseEditState {
    data object Loading : CourseEditState
    data class Success(
        val courseInfo: CourseEntity,
        val recordInfo: List<CourseRecordEntity>
    ) : CourseEditState
}

sealed interface CourseEditSideEffect {

}