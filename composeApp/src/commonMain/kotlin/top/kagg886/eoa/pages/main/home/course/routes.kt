package top.kagg886.eoa.pages.main.home.course

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailScreen
import top.kagg886.eoa.pages.main.home.course.list.CourseListRoute
import top.kagg886.eoa.pages.main.home.course.list.CourseListScreen
import top.kagg886.eoa.pages.main.home.course.manage.CourseManageRoute
import top.kagg886.eoa.pages.main.home.course.manage.installCourseManageRoute
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object CourseRoute

val installCourseGraph: NavGraphBuilder.() -> Unit = {
    composable<CourseListRoute> { AutoInject { CourseListScreen() } }
    composable<CourseDetailRoute> { AutoInject { CourseDetailScreen(it.toRoute()) } }
    navigation<CourseManageRoute>(startDestination = CourseManageListRoute, builder = installCourseManageRoute)
}
