package top.kagg886.eoa.pages.main.home

import StackedSnackbarDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.adaptive.NavigationSuiteScaffold
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.main.MainRouteViewEffect
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.gpa.GPARoute
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.settings.SettingsRoute
import top.kagg886.eoa.util.SnackBarType.*
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.replace
import top.kagg886.eoa.util.showSnackBar

@Composable
fun HomeScreen(
    route: NavigationRoute,
    enableNavigation: Boolean = true,
    menu: @Composable (() -> Unit)? = {
        val nav = LocalNavController.current
        IconButton(
            onClick = {
                nav.navigate(SettingsRoute)
            },
        ) {
            Icon(Icons.Default.AccountBox, contentDescription = "返回")
        }
    },
    back: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    fabIcon: @Composable (() -> Unit)? = null,
    fabText: @Composable (() -> Unit)? = null,
    fabOnClick: () -> Unit = {},
    fabModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
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
            is MainRouteViewEffect.NavigateToLogin -> {
                nav.navigate(LoginRoute) {
                    popUpTo(0) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }
    NavigationSuiteScaffold(
        enableNavigation = enableNavigation,
        modifier = modifier,
        navigationSuiteItems = {
            menu(menu)
            title(title)
            back(back)

            fab(onClick = fabOnClick, icon = fabIcon, text = fabText, modifier = fabModifier)

            for (navigationRoute in NavigationRoute.entries) {
                item(
                    selected = navigationRoute == route,
                    onClick = {
                        if (navigationRoute != route) {
                            nav.replace(navigationRoute.target)
                        }
                    },
                    icon = {
                        Icon(
                            navigationRoute.icon,
                            contentDescription = navigationRoute.display
                        )
                    },
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

    GPA(
        target = GPARoute,
        display = "绩点",
        icon = Icons.Default.Star
    )
}
