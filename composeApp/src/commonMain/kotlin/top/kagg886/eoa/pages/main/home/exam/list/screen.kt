package top.kagg886.eoa.pages.main.home.exam.list

import androidx.compose.runtime.Composable
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailRoute
import top.kagg886.eoa.pages.main.home.exam.export.ExamExportRoute
import top.kagg886.eoa.pages.main.home.exam.list.filter.ExamListFilterRoute
import top.kagg886.eoa.pages.main.home.exam.statistic.ExamStatisticRoute

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/6 15:36
 * ================================================
 */

@Composable
fun ExamListScreen(content: @Composable () -> Unit) {
    val model = examListViewModelOrNull()
    val nav = LocalNavController.current

    model?.collectSideEffect {
        when (it) {
            is ExamListSideEffect.NavigateToDetail -> {
                nav.navigate(ExamDetailRoute(it.examId))
            }

            is ExamListSideEffect.NavigateToStatistic -> {
                nav.navigate(ExamStatisticRoute(it.year, it.term))
            }

            is ExamListSideEffect.NavigateToExport -> {
                nav.navigate(ExamExportRoute(it.year, it.term))
            }

            ExamListSideEffect.NavigateToFilter -> {
                nav.navigate(ExamListFilterRoute)
            }
        }
    }

    content()
}
