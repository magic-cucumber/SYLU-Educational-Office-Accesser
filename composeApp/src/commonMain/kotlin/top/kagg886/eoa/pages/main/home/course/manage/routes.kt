package top.kagg886.eoa.pages.main.home.course.manage

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.manage.edit.CourseEditRoute
import top.kagg886.eoa.pages.main.home.course.manage.edit.CourseEditScreen
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListScreen
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object CourseManageRoute


val installCourseManageRoute: NavGraphBuilder.() -> Unit = {
    composable<CourseManageListRoute> { AutoInject { CourseManageListScreen() } }
    dialog<CourseEditRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        CourseEditScreen(
            it.toRoute()
        )
    }
}