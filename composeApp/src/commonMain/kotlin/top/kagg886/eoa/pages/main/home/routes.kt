package top.kagg886.eoa.pages.main.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.course.installCourseGraph
import top.kagg886.eoa.pages.main.home.course.list.CourseListRoute
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.exam.installExamGraph
import top.kagg886.eoa.pages.main.home.exam.list.ExamListRoute
import top.kagg886.eoa.pages.main.home.gpa.GPARoute
import top.kagg886.eoa.pages.main.home.gpa.GPAScreen
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.home.summary.SummaryScreen
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object HomeRoute

val installHomeGraph: NavGraphBuilder.() -> Unit = {
    composable<SummaryRoute> { AutoInject { SummaryScreen() } }
    navigation<CourseRoute>(startDestination = CourseListRoute, builder = installCourseGraph)
    navigation<ExamRoute>(startDestination = ExamListRoute, builder = installExamGraph)
    composable<GPARoute> { GPAScreen() }
}
