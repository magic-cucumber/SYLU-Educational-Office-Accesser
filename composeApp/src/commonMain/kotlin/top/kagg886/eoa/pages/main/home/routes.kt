package top.kagg886.eoa.pages.main.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.course.installCourseGraph
import top.kagg886.eoa.pages.main.home.course.list.CourseListRoute
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.exam.installExamGraph
import top.kagg886.eoa.pages.main.home.exam.list.ExamListRoute
import top.kagg886.eoa.pages.main.home.gpa.GPARoute
import top.kagg886.eoa.pages.main.home.gpa.GPAScreen
import top.kagg886.eoa.pages.main.home.notice.SystemNoticeRoute
import top.kagg886.eoa.pages.main.home.notice.SystemNoticeScreen
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.home.summary.SummaryScreen
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object HomeRoute

val installHomeGraph: NavGraphBuilder.() -> Unit = {
    composable<SummaryRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        AutoInject {
            SummaryScreen()
        }
    }
    dialog<SystemNoticeRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { SystemNoticeScreen() }
    navigation<CourseRoute>(startDestination = CourseListRoute, builder = installCourseGraph)
    navigation<ExamRoute>(startDestination = ExamListRoute, builder = installExamGraph)
    composable<GPARoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        GPAScreen()
    }
}
