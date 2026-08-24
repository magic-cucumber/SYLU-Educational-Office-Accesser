package top.kagg886.eoa.pages.main.home.exam.detail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import top.kagg886.eoa.util.BaseViewModel
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.pages.main.MainRouteViewState

class ExamDetailViewModel(
    private val recordId: Long,
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : BaseViewModel<ExamDetailState, ExamDetailSideEffect>(name = "ExamDetailViewModel", initial = ExamDetailState.Loading) {
    private val examDao = database.examDao()


    override suspend fun Syntax<ExamDetailState, ExamDetailSideEffect>.init() {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return
                }
                // 否则提示同步失败
                reduce {
                    ExamDetailState.Failed(syncState.message)
                }
                return
            }

            // 正在同步则展示加载中
            if (syncState is MainRouteViewState.SyncProcess) {
                // 如果有脏数据则展示
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return
                }
                // 否则展示加载中
                reduce {
                    ExamDetailState.Loading
                }
                return
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                setDataUnsafe().join()
                return
            }
    }

    fun setDataUnsafe() = intent {
        val exam = examDao.getById(recordId)!!

        val terms = AppSyncMMKV.picker!!.list
        val timeline = examDao.getTimeLineByCourseId(exam.courseID)!!.map { timeLine ->
            timeLine.copy(
                year = terms.first { it.asTerm().xnm == timeLine.year }.asDisplay().xnm,
                semester = terms.first { it.asTerm().xqm == timeLine.semester }.asDisplay().xqm,
            )
        }

        reduce {
            ExamDetailState.Success(
                records = timeline.first { it.id == recordId },
                timeline = timeline
            )
        }
    }

}


sealed interface ExamDetailState {
    data class Success(
        val records: ExamEntity,
        val timeline: List<ExamEntity>
    ) : ExamDetailState

    data class Failed(val msg: String) : ExamDetailState
    data object Loading : ExamDetailState
}

sealed interface ExamDetailSideEffect {
    data class ShowToast(val message: String) : ExamDetailSideEffect
}
