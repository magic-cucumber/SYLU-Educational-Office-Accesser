package top.kagg886.eoa.pages.main.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.adaptive.NavigationSuiteScaffold
import top.kagg886.eoa.component.adaptive.NavigationSuiteScope
import top.kagg886.eoa.util.isNavigationBar
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.home.course.CourseRoute
import top.kagg886.eoa.pages.main.home.exam.ExamRoute
import top.kagg886.eoa.pages.main.home.gpa.GPARoute
import top.kagg886.eoa.pages.main.home.link.list.LinkListRoute
import top.kagg886.eoa.pages.main.home.second.SecondClassRoute
import top.kagg886.eoa.pages.main.home.summary.SummaryRoute
import top.kagg886.eoa.pages.main.settings.SettingsRoute
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.currentLayoutType

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    route: EOAHomeModule,
    enableNavigation: Boolean = true,
    layoutType: NavigationSuiteType = currentLayoutType(),
    suite: (NavigationSuiteScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) = MainScreen {
    val nav = LocalNavController.current
    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val homeModule by rootState.module.collectAsState()
    val allowModule by rootState.allowModule.collectAsState()
    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        suite = {
            menu {
                IconButton(onClick = { nav.navigate(SettingsRoute) }) {
                    Icon(Icons.Default.AccountBox, contentDescription = "返回")
                }
            }
            navigationItems { navigationType ->
                val itemModifier = if (navigationType == NavigationSuiteType.NavigationDrawer) {
                    Modifier.padding(horizontal = 16.dp)
                } else {
                    Modifier
                }
                for (navigationRoute in homeModule) {
                    NavigationSuiteItem(
                        navigationSuiteType = navigationType,
                        modifier = itemModifier,
                        enabled = enableNavigation,
                        selected = navigationRoute == route,
                        onClick = {
                            if (navigationRoute != route) {
                                nav.navigate(navigationRoute.target) {
                                    popUpTo(MainRoute)
                                    launchSingleTop = true
                                }
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

                val otherModule = allowModule - homeModule.toSet()
                if (otherModule.isNotEmpty()) {
                    var popMenu by remember { mutableStateOf(false) }
                    NavigationSuiteItem(
                        navigationSuiteType = navigationType,
                        modifier = itemModifier,
                        enabled = enableNavigation,
                        selected = false,
                        onClick = {
                            popMenu = !popMenu
                        },
                        icon = {
                            Icon(
                                if (layoutType.isNavigationBar) Icons.Default.MoreHoriz else Icons.Default.MoreVert,
                                contentDescription = "更多"
                            )
                            if (popMenu) {
                                DropdownMenu(
                                    expanded = popMenu,
                                    onDismissRequest = { popMenu = false },
                                ) {
                                    for (i in otherModule) {
                                        DropdownMenuItem(
                                            text = { Text(i.display) },
                                            leadingIcon = { Icon(i.icon, contentDescription = i.display) },
                                            onClick = {
                                                popMenu = false
                                                nav.navigate(i.target) {
                                                    popUpTo(MainRoute)
                                                    launchSingleTop = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        label = { Text("更多") },
                    )
                }
            }
            suite?.invoke(this)
        },
        content = content
    )
}

typealias EOAHomeModule = top.kagg886.backend.config.EOAHomeModule

val EOAHomeModule.target: Any
    get() = when (this) {
        EOAHomeModule.SUMMARY -> SummaryRoute
        EOAHomeModule.COURSE -> CourseRoute
        EOAHomeModule.EXAM -> ExamRoute
        EOAHomeModule.GPA -> GPARoute
        EOAHomeModule.SECOND -> SecondClassRoute
        EOAHomeModule.LINK -> LinkListRoute
    }

val EOAHomeModule.display: String
    get() = when (this) {
        EOAHomeModule.SUMMARY -> "首页"
        EOAHomeModule.COURSE -> "课表"
        EOAHomeModule.EXAM -> "考试"
        EOAHomeModule.GPA -> "绩点"
        EOAHomeModule.SECOND -> "二课"
        EOAHomeModule.LINK -> "友链"
    }

val EOAHomeModule.icon: ImageVector
    get() = when (this) {
        EOAHomeModule.SUMMARY -> Icons.Default.Home
        EOAHomeModule.COURSE -> Icons.Default.CalendarMonth
        EOAHomeModule.EXAM -> Icons.Default.Bookmark
        EOAHomeModule.GPA -> Icons.Default.Star
        EOAHomeModule.SECOND -> Icons.Default.Accessibility
        EOAHomeModule.LINK -> Icons.Default.Link
    }
