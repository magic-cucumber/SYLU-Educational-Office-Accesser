package top.kagg886.eoa.pages.main.home.gpa

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.GPAEntity
import top.kagg886.backend.database.dao.GPASummaryEntity
import top.kagg886.eoa.pages.main.MainRouteViewState

class GPAViewModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), OrbitContainerHost<GPAState, GPAState, GPAEffect> {
    private val gpaSummaryDao = database.gpaSummaryDao()
    private val gpaDao = database.gpaDao()

    override val container = orbitContainer<GPAState, GPAEffect>(GPAState.Loading) {
        if (syncState is MainRouteViewState.SyncFailed) {
            // 非首次同步则展示脏数据
            if (syncState.haveDirtyData) {
                setDataUnsafe().join()
                return@orbitContainer
            }
            // 否则提示同步失败
            reduce {
                GPAState.Failed(syncState.message)
            }
            return@orbitContainer
        }

        // 正在同步则展示加载中
        if (syncState is MainRouteViewState.SyncProcess) {
            // 如果有脏数据则展示
            if (syncState.haveDirtyData) {
                setDataUnsafe().join()
                return@orbitContainer
            }
            // 否则展示加载中
            reduce {
                GPAState.Loading
            }
            return@orbitContainer
        }

        // 同步成功则展示数据
        if (syncState is MainRouteViewState.SyncSuccess) {
            setDataUnsafe().join()
            return@orbitContainer
        }
    }

    private fun setDataUnsafe() = intent {
        val summary = gpaSummaryDao.all()
        val data = gpaDao.all()

        val s = data.groupBy { entity-> summary.first { entity.summaryId == it.id } }

        reduce {
            GPAState.Success(s)
        }
    }
}


sealed interface GPAState {
    data object Loading : GPAState
    data class Success(
        val gpa: Map<GPASummaryEntity, List<GPAEntity>>
    ): GPAState

    data class Failed(val msg: String) : GPAState
    data class FailedButSuccess(
        val msg: String
    ) : GPAState
}

sealed interface GPAEffect {
}
