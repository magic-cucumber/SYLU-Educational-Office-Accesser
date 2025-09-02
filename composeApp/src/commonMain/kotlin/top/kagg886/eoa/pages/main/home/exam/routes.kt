package top.kagg886.eoa.pages.main.home.exam

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.component.nav.transition
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailRoute
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailScreen
import top.kagg886.eoa.pages.main.home.exam.list.ExamListRoute
import top.kagg886.eoa.pages.main.home.exam.list.ExamListScreen
import top.kagg886.eoa.pages.main.home.exam.statistic.ExamStatisticRoute
import top.kagg886.eoa.pages.main.home.exam.statistic.ExamStatisticScreen

@Serializable
data object ExamRoute

val installExamGraph: NavGraphBuilder.() -> Unit = {
    transition<ExamListRoute> { ExamListScreen() }
    transition<ExamDetailRoute> { ExamDetailScreen(it.toRoute()) }
    dialog<ExamStatisticRoute> { ExamStatisticScreen(it.toRoute()) }
}
