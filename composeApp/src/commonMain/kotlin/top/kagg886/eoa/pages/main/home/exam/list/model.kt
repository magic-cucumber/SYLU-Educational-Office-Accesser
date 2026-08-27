package top.kagg886.eoa.pages.main.home.exam.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.LocalDatabase
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.home.exam.list.content.*
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.sylu_eoa.api.v2.bean.TERM_ALL_PICKER

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/6 15:35
 * ================================================
 */

@Composable
fun examListViewModelOrNull(): ExamListViewModel? {
    val mainViewModel = mainViewModelOrNull() ?: return null
    val syncState by mainViewModel.collectAsState()

    val nav = LocalNavController.current
    val state by nav.currentBackStackEntryAsState()
    val parentEntry = remember(state) {
        runCatching { nav.getBackStackEntry(ExamListRoute) }.getOrNull() // 嵌套图 route
    }

    if (parentEntry == null) {
        return null
    }

    val database = LocalDatabase.current

    return viewModel(parentEntry,key = syncState.toString()) {
        ExamListViewModel(database, syncState)
    }
}


class ExamListViewModel(
    database: AppDatabase,
    private val syncState: MainRouteViewState
) : BaseViewModel<ExamListState, ExamListSideEffect>(name = "ExamListViewModel", initial = ExamListState.Loading) {
    private val examDao = database.examDao()



    fun navigateToFilter() = intent {
        postSideEffect(ExamListSideEffect.NavigateToFilter)
    }

    fun navigateToDetail(it: ExamEntity) = intent {
        postSideEffect(ExamListSideEffect.NavigateToDetail(it.id!!))
    }

    @OptIn(OrbitExperimental::class)
    fun navigateToStatistic() = intent {
        runOn<ExamListState.Success> {
            val (year, terms) = state.selector[state.currentYearIndex]
            val term = terms[state.currentTermIndex]

            postSideEffect(
                ExamListSideEffect.NavigateToStatistic(year.yearCode, term.semesterCode)
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun navigateToExport() = intent {
        runOn<ExamListState.Success> {
            val (year, terms) = state.selector[state.currentYearIndex]
            val term = terms[state.currentTermIndex]

            postSideEffect(
                ExamListSideEffect.NavigateToExport(year.yearCode, term.semesterCode)
            )
        }
    }

    override suspend fun Syntax<ExamListState, ExamListSideEffect>.init() {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    filterPassType().join()
                    return
                }
                // 否则提示同步失败
                reduce {
                    ExamListState.Failed(syncState.message)
                }
                return
            }

            // 正在同步则展示加载中
            if (syncState is MainRouteViewState.SyncProcess) {
                // 如果有脏数据则展示
                if (syncState.haveDirtyData) {
                    filterPassType().join()
                    return
                }
                // 否则展示加载中
                reduce {
                    ExamListState.Loading
                }
                return
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                filterPassType().join()
                return
            }
    }

    fun filterPassType(
        keyword: String? = null,
        type: PassFilter = PassFilter.ALL,
        degree: DegreeFilter = DegreeFilter.ALL,
        currentYearIndex: Int? = null,
        currentTermIndex: Int? = null,
    ) = intent {
        val originTerms = (state as? ExamListState.Success)?.selector

        reduce {
            ExamListState.Loading
        }

        val picker = AppSyncMMKV.picker ?: return@intent
        withContext(Dispatchers.IO) {
            // Map<学年，该学年的所有学期>
            // 复用之前的对象，防止重复计算
            val terms = originTerms ?: listOf(TERM_ALL_PICKER)
                .plus(picker.list)
                .plus(
                    picker.list.map { i ->
                        TERM_ALL_PICKER.copy(yearName = i.asDisplay().xnm to i.asTerm().xnm)
                    }
                )
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
                .map { it.key to it.value.toSet().map { it.second } }

            //默认值为当前学年学期的代号
            val year = currentYearIndex
                ?: terms.indexOfFirst { picker.default.asTerm().xnm == it.first.yearCode }
            val term = currentTermIndex
                ?: terms[year].second.indexOfFirst { picker.default.asTerm().xqm == it.semesterCode }

            val selectYear = terms[year]
            val selectSemester = selectYear.second[term]

            val list = examDao.all(
                keyword,
                type.toExamStatus(),
                degree.toQuery(),
                selectYear.first.yearCode.ifEmpty { null }, // '全部' 的code为空，转成null让数据库返回正常数据
                selectSemester.semesterCode.ifEmpty { null },
            )


            reduce {
                ExamListState.Success(
                    keyword = keyword,
                    passFilter = type,
                    degreeFilter = degree,
                    entity = list,
                    selector = terms,
                    currentYearIndex = year,
                    currentTermIndex = term,
                    lazyListState = LazyListState()
                )
            }
        }
    }

}

sealed interface ExamListState {

    data class Success(
        val lazyListState: LazyListState,

        val keyword: String?,
        val passFilter: PassFilter,
        val degreeFilter: DegreeFilter,
        val entity: List<ExamEntity>,
        val selector: List<Pair<YearSelectBean, List<TermSelectBean>>>,

        val currentYearIndex: Int,
        val currentTermIndex: Int,
    ) : ExamListState

    data class Failed(val msg: String) : ExamListState

    data object Loading : ExamListState
}

sealed interface ExamListSideEffect {
    data object NavigateToFilter : ExamListSideEffect
    data class NavigateToDetail(val examId: Long) : ExamListSideEffect
    data class NavigateToStatistic(val year: String, val term: String) : ExamListSideEffect
    data class NavigateToExport(val year: String, val term: String) : ExamListSideEffect
}
