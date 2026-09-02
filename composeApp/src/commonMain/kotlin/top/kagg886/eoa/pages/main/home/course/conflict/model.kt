package top.kagg886.eoa.pages.main.home.course.conflict

import kotlinx.datetime.LocalDateTime
import top.kagg886.eoa.util.BaseViewModel
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.backend.database.dao.CourseRecordEntity

class CourseConflictViewModel(
    database: AppDatabase,
    private val startTime: LocalDateTime,
    private val endTime: LocalDateTime,
) : BaseViewModel<CourseConflictState, CourseConflictSideEffect>(name = "CourseConflictViewModel", initial = CourseConflictState.Loading) {
    private val courseRecordDao = database.courseRecordDao()
    override suspend fun Syntax<CourseConflictState, CourseConflictSideEffect>.init() {
            val date = courseRecordDao.getCoursesWithRecordInfo(startTime,endTime)
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
