package top.kagg886.eoa.pages.main.home.exam

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailRoute
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailScreen
import top.kagg886.eoa.pages.main.home.exam.list.ExamListRoute
import top.kagg886.eoa.pages.main.home.exam.list.ExamListScreen
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object ExamRoute

val installExamGraph: NavGraphBuilder.() -> Unit = {
    composable<ExamListRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        AutoInject {
            ExamListScreen()
        }
    }
    composable<ExamDetailRoute> { AutoInject { ExamDetailScreen(it.toRoute()) } }
}
