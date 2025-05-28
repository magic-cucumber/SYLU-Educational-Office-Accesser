package top.kagg886.eoa.pages.main.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute


@Composable
fun HomeScreen(
    route: NavigationRoute,
    content: @Composable () -> Unit
) {
    val nav = LocalNavController.current
//    NavigationSuiteScaffold(
//        navigationSuiteItems = {
//            for (navigationRoute in NavigationRoute.entries) {
//                item(
//                    selected = navigationRoute == route,
//                    onClick = { nav.navigate(navigationRoute.target) },
//                    icon = { Icon(navigationRoute.icon, contentDescription = navigationRoute.display) },
//                    label = { Text(navigationRoute.display) }
//                )
//            }
//        },
//        layoutType = currentLayoutType(),
//        content = content
//    )
}

enum class NavigationRoute(val target: Any, val display: String, val icon: ImageVector) {
    SUMMARY(
        target = SummaryRoute,
        display = "首页",
        icon = Icons.Default.Home
    ),

    COURSE(
        target = CourseRoute,
        display = "课表",
        icon = Icons.Default.CalendarMonth
    ),

    EXAM(
        target = ExamRoute,
        display = "考试",
        icon = Icons.Default.Bookmark
    ),
}
