package top.kagg886.eoa.pages.main.home.exam.list

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.sylu_eoa.api.v2.bean.TERM_ALL_PICKER

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
                    ExamListState.Failed(state.drawerState, syncState.message)
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

    fun filterPassType(
        type: PassFilter = PassFilter.ALL,
        degree: DegreeFilter = DegreeFilter.ALL,
        currentYearIndex:Int? = null,
        currentTermIndex:Int? = null,
    ) = intent {

        reduce {
            ExamListState.Loading(state.drawerState)
        }

        withContext(Dispatchers.IO) {
            // Map<学年，该学年的所有学期>
            // 复用之前的对象，防止重复计算
            val terms = (state as? ExamListState.Success)?.selector ?: mutableListOf(TERM_ALL_PICKER)
                .plus(AppSyncMMKV.picker!!.list)
                .map {
                    YearSelectBean(
                        it.asDisplay().xnm,
                        it.asTerm().xnm
                    ) to TermSelectBean(
                        it.asDisplay().xqm,
                        it.asTerm().xqm
                    )
                }
                .groupBy { it.first }
                .map { it.key to it.value.map { it.second } }

            //默认值为当前学年学期的代号
            val year = currentYearIndex ?: terms.indexOfFirst { AppSyncMMKV.picker!!.default.asTerm().xnm == it.first.yearCode }
            val term = currentTermIndex ?: terms[year].second.indexOfFirst { AppSyncMMKV.picker!!.default.asTerm().xqm == it.semesterCode }

            val selectYear = terms[year]
            val selectSemester = selectYear.second[term]

            val list = examDao.all(
                type.toExamStatus(),
                degree.toQuery(),
                selectYear.first.yearCode.ifEmpty { null }, // '全部' 的code为空，转成null让数据库返回正常数据
                selectSemester.semesterCode.ifEmpty { null },
            )


            reduce {
                ExamListState.Success(
                    passFilter = type,
                    degreeFilter = degree,
                    entity = list,
                    selector = terms,
                    currentYearIndex = year,
                    currentTermIndex = term,
                    drawerState = state.drawerState,
                )
            }
        }
    }

}

sealed interface ExamListState {
    val drawerState: DrawerState

    data class Success(
        val passFilter: PassFilter,
        val degreeFilter: DegreeFilter,
        val entity: List<ExamEntity>,
        val selector: List<Pair<YearSelectBean, List<TermSelectBean>>>,

        val currentYearIndex: Int,
        val currentTermIndex: Int,

        override val drawerState: DrawerState
    ) : ExamListState

    data class Failed(
        override val drawerState: DrawerState,
        val msg: String
    ) : ExamListState

    data class Loading(
        override val drawerState: DrawerState
    ) : ExamListState
}

sealed interface ExamListSideEffect {
    data class NavigateToDetail(val examId: Long) : ExamListSideEffect
}