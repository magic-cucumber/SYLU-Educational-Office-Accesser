package top.kagg886.eoa.pages.main.home.course.conflict

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.backend.database.dao.CourseRecordEntity

class CourseConflictViewModel(
    database: AppDatabase,
    weekNumber: Int,
    dayOfWeek: Int,
    periodOfDay: Int
) : ViewModel(), OrbitContainerHost<CourseConflictState, CourseConflictState, CourseConflictSideEffect> {
    private val courseRecordDao = database.courseRecordDao()
    override val container =
        orbitContainer<CourseConflictState, CourseConflictSideEffect>(CourseConflictState.Loading) {
            val date = courseRecordDao.getCoursesWithRecordInfo(weekNumber, dayOfWeek, periodOfDay)
            reduce {
                CourseConflictState.Success(date)
            }
        }

    fun navigateTo(record:CourseRecordEntity) = intent {
        postSideEffect(CourseConflictSideEffect.NavigateToDetail(record.id!!))
    }
}

sealed interface CourseConflictState {
    data object Loading : CourseConflictState
    data class Success(
        val course: List<CourseAndRecord>
    ) : CourseConflictState
}

sealed interface CourseConflictSideEffect {
    data class NavigateToDetail(val recordId: Long) : CourseConflictSideEffect
}
