package top.kagg886.eoa.pages.main.home.course

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictRoute
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictScreen
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailScreen
import top.kagg886.eoa.pages.main.home.course.export_calender.CourseExportCalenderRoute
import top.kagg886.eoa.pages.main.home.course.export_calender.CourseExportCalenderScreen
import top.kagg886.eoa.pages.main.home.course.export_ics.CourseExportIcsScreen
import top.kagg886.eoa.pages.main.home.course.export_ics.CourseExportIcsRoute
import top.kagg886.eoa.pages.main.home.course.list.CourseListRoute
import top.kagg886.eoa.pages.main.home.course.list.CourseListScreen
import top.kagg886.eoa.pages.main.home.course.manage.CourseManageRoute
import top.kagg886.eoa.pages.main.home.course.manage.installCourseManageRoute
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import top.kagg886.eoa.util.shared.AutoInject

@Serializable
data object CourseRoute

val installCourseGraph: NavGraphBuilder.() -> Unit = {
    composable<CourseListRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        AutoInject {
            CourseListScreen()
        }
    }
    composable<CourseDetailRoute> { AutoInject { CourseDetailScreen(it.toRoute()) } }
    navigation<CourseManageRoute>(
        startDestination = CourseManageListRoute,
        builder = installCourseManageRoute
    )
    dialog<CourseConflictRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { CourseConflictScreen(it.toRoute()) }

    dialog<CourseExportIcsRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {  CourseExportIcsScreen() }
    dialog<CourseExportCalenderRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { CourseExportCalenderScreen() }
}
