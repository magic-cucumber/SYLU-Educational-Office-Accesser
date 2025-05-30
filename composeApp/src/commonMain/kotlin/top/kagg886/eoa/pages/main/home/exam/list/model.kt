package top.kagg886.eoa.pages.main.home.exam.list

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.pages.main.MainRouteViewState

class ExamListViewModel(
    database: AppDatabase,
    private val syncState: MainRouteViewState
) : ViewModel(), ContainerHost<ExamListState, ExamListSideEffect> {
    private val examDao = database.examDao()


    fun navigateToDetail(it: ExamEntity) = intent {
        postSideEffect(ExamListSideEffect.NavigateToDetail(it.id!!))
    }

    override val container: Container<ExamListState, ExamListSideEffect> =
        container(ExamListState.Loading(DrawerState(DrawerValue.Closed))) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    filterPassType().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    ExamListState.Failed(state.drawerState)
                }
                return@container
            }

            // 正在同步则展示加载中
            if (syncState is MainRouteViewState.SyncProcess) {
                // 如果有脏数据则展示
                if (syncState.haveDirtyData) {
                    filterPassType().join()
                    return@container
                }
                // 否则展示加载中
                reduce {
                    ExamListState.Loading(state.drawerState)
                }
                return@container
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                filterPassType().join()
                return@container
            }
        }

    fun filterPassType(type: PassFilter = PassFilter.ALL, degree: DegreeFilter = DegreeFilter.ALL) = intent {
        val state = DrawerState(DrawerValue.Closed)
        reduce {
            ExamListState.Loading(state)
        }
        val list = examDao.all(type.toExamStatus(), degree.toQuery())
        reduce {
            ExamListState.Success(
                type,
                degree,
                list,
                state
            )
        }
    }

}

sealed interface ExamListState {
    val drawerState:DrawerState

    data class Success(
        val passFilter: PassFilter,
        val degreeFilter: DegreeFilter,
        val entity: List<ExamEntity>,
        override val drawerState:DrawerState
    ) : ExamListState

    data class Failed(
        override val drawerState:DrawerState
    ) : ExamListState
    data class Loading(
        override val drawerState:DrawerState
    ) : ExamListState
}

sealed interface ExamListSideEffect {
    data class NavigateToDetail(val examId: Long) : ExamListSideEffect
}