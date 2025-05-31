package top.kagg886.eoa.pages.main.home.course.manage

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListScreen
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object CourseManageRoute


val installCourseManageRoute:  NavGraphBuilder.() -> Unit = {
    composable<CourseManageListRoute> { AutoInject { CourseManageListScreen() } }
}