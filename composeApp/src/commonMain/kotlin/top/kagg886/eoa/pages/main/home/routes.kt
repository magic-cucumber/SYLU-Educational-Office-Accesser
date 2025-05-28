package top.kagg886.eoa.pages.main.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.course.CourseScreen
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.exam.ExamScreen
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.home.summary.SummaryScreen

@Serializable
data object HomeRoute

val installHomeGraph: NavGraphBuilder.() -> Unit = {
    composable<SummaryRoute> { SummaryScreen() }
    composable<CourseRoute> { CourseScreen() }
    composable<ExamRoute> { ExamScreen() }
}
