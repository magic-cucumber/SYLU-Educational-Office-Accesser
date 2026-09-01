package top.kagg886.eoa.pages.main.home.course

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import top.kagg886.eoa.component.nav.transition
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictRoute
import top.kagg886.eoa.pages.main.home.course.conflict.CourseConflictScreen
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailScreen
import top.kagg886.eoa.pages.main.home.course.export_calender.CourseExportCalenderRoute
import top.kagg886.eoa.pages.main.home.course.export_calender.CourseExportCalenderScreen
import top.kagg886.eoa.pages.main.home.course.export_ics.CourseExportIcsRoute
import top.kagg886.eoa.pages.main.home.course.export_ics.CourseExportIcsScreen
import top.kagg886.eoa.pages.main.home.course.list.CourseListRoute
import top.kagg886.eoa.pages.main.home.course.list.CourseListScreen
import top.kagg886.eoa.pages.main.home.course.manage.CourseManageRoute
import top.kagg886.eoa.pages.main.home.course.manage.installCourseManageRoute
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import kotlin.reflect.typeOf

@Serializable
data object CourseRoute


val installCourseGraph: NavGraphBuilder.() -> Unit = {
    transition<CourseListRoute> { CourseListScreen() }
    transition<CourseDetailRoute>(
        deepLinks = listOf(
            navDeepLink<CourseDetailRoute>(basePath = "eoa://course/profile")
        )
    ) { CourseDetailScreen(it.toRoute()) }
    navigation<CourseManageRoute>(
        startDestination = CourseManageListRoute,
        builder = installCourseManageRoute
    )
    dialog<CourseConflictRoute>(
        typeMap = mapOf(typeOf<LocalDateTime>() to CourseConflictRoute.Type),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        deepLinks = listOf(
            navDeepLink<CourseConflictRoute>(
                basePath = "eoa://course/conflict",
                typeMap = mapOf(typeOf<LocalDateTime>() to CourseConflictRoute.Type)
            )
        )
    ) {
        CourseConflictScreen(
            it.toRoute()
        )
    }

    dialog<CourseExportIcsRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { CourseExportIcsScreen() }
    dialog<CourseExportCalenderRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { CourseExportCalenderScreen() }
}
