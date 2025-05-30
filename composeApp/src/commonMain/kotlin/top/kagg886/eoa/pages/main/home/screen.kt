package top.kagg886.eoa.pages.main.home

import StackedSnackbarDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.adaptive.NavigationSuiteScaffold
import top.kagg886.eoa.pages.main.MainRouteViewEffect
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType.*
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.showSnackBar

@Composable
fun HomeScreen(
    route: NavigationRoute,
    enableNavigation: Boolean = true,
    menu: @Composable (() -> Unit)? = null,
    back: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    fabIcon: @Composable (() -> Unit)? = null,
    fabText: @Composable (() -> Unit)? = null,
    fabOnClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val nav = LocalNavController.current
    val model = mainViewModel()

    val snack = LocalSnackBarHost.current
    model.collectSideEffect { effect ->
        when (effect) {
            is MainRouteViewEffect.Toast -> {
                snack.showSnackBar(
                    type = effect.type,
                    title = when (effect.type) {
                        Success -> "成功"
                        Warning -> "警告"
                        Error -> "错误"
                        Info -> "信息"
                    },
                    description = effect.message,
                    duration = StackedSnackbarDuration.Short
                )
            }
        }
    }
    NavigationSuiteScaffold(
        enableNavigation = enableNavigation,
        navigationSuiteItems = {
            menu(menu)
            title(title)
            back(back)

            fab(onClick = fabOnClick, icon = fabIcon, text = fabText)

            for (navigationRoute in NavigationRoute.entries) {
                item(
                    selected = navigationRoute == route,
                    onClick = {
                        if (navigationRoute != route) {
                            nav.navigate(navigationRoute.target)
                        }
                    },
                    icon = { Icon(navigationRoute.icon, contentDescription = navigationRoute.display) },
                    label = { Text(navigationRoute.display) },
                )
            }
        },
        layoutType = currentLayoutType(),
        content = content
    )
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
