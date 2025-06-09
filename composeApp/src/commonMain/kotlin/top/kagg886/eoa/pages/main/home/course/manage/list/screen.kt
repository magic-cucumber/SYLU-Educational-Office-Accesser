package top.kagg886.eoa.pages.main.home.course.manage.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.home.course.export.CourseExportRoute
import top.kagg886.eoa.pages.main.home.course.manage.edit.CourseEditRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed

@Serializable
data object CourseManageListRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CourseManageListScreen() {
    val mainModel = mainViewModel()
    val mainState by mainModel.collectAsState()
    val model = viewModel {
        CourseManageListModel(mainState, mainModel.database)
    }
    val nav = LocalNavController.current
    val state by model.collectAsState()

    HomeScreen(
        route = NavigationRoute.COURSE,
        back = { BackIconButton() },
        title = { Text("管理课程") },
        menu = {
            var showDropdownMenu by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showDropdownMenu = true },
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            }
            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = { showDropdownMenu = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        model.toggleSystemCourseVisible()
                        showDropdownMenu = false
                    },
                    text = { Text("${if ((state as? CourseManageState.Success)?.onlyShowUserCourse == true) "显示" else "隐藏"}系统课程") },
                )

                DropdownMenuItem(
                    onClick = {
                        model.startExportICS()
                    },
                    text = { Text("保存为ICS文件") }
                )
            }
        },
        fabIcon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        },
        fabText = { Text("添加课程") },
        fabOnClick = { model.openAddOrEditCourse(null) }
    ) {
        model.collectSideEffect {
            when (it) {
                is CourseManageSideEffect.NavigateToEditOrAdd -> {
                    nav.navigate(CourseEditRoute(it.courseId))
                }

                is CourseManageSideEffect.Toast -> {
                    mainModel.toast(SnackBarType.Error, it.msg)
                }

                is CourseManageSideEffect.StartExportIcs -> {
                    nav.navigate(CourseExportRoute)
                }
            }
        }
        Surface(
            Modifier.fillMaxSize().shareElementComposed(
                sharedContentState = rememberSharedContentState(key = "list-course-to-manage-course"),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            )
        ) {
            CoursePageScreenContent(
                state = state,
                onCourseItemClicked = {
                    model.openAddOrEditCourse(it)
                },
                onCourseItemDeleted = {
                    model.deleteCourse(it)
                }
            )
        }
    }
}

@Composable
private fun CoursePageScreenContent(
    state: CourseManageState,
    onCourseItemClicked: (CourseEntity) -> Unit,
    onCourseItemDeleted: (CourseEntity) -> Unit
): Unit = when (state) {
    is CourseManageState.Failed -> {
        ErrorPage(
            title = { Text("获取课表失败") },
            message = { Text(state.msg) },
        )
    }

    is CourseManageState.FailedButSuccess -> {
        ErrorPage(
            modifier = Modifier.fillMaxSize(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "提示",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            message = {
                Text(
                    text = state.msg,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

            }
        )
    }

    CourseManageState.Loading -> CoursePageScreenSuccessContent(null, {}) {}
    is CourseManageState.Success -> CoursePageScreenSuccessContent(
        state,
        onCourseItemClicked,
        onCourseItemDeleted
    )
}

@Composable
private fun CoursePageScreenSuccessContent(
    state: CourseManageState.Success?,
    onCourseItemClicked: (CourseEntity) -> Unit,
    onCourseItemRemoveClicked: (CourseEntity) -> Unit
) {
    val visible by remember(state) {
        derivedStateOf {
            state == null
        }
    }
    if (state?.data?.isEmpty() == true) {
        ErrorPage(
            title = { Text("暂无数据") },
            message = { Text("请修改筛选器后重试") },
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    if (currentLayoutType() == NavigationSuiteType.NavigationBar) {
        //在安卓上不知道为什么会崩溃，先改成滑动删除/编辑先
        LazyColumn {
            items(state?.data ?: List(6) { null }) {
                val swipeState = rememberSwipeToDismissBoxState()
                LaunchedEffect(swipeState.currentValue) {
                    when (swipeState.currentValue) {
                        SwipeToDismissBoxValue.EndToStart -> {
                            onCourseItemRemoveClicked(it!!)
                        }

                        SwipeToDismissBoxValue.StartToEnd -> {
                            onCourseItemClicked(it!!)
                        }

                        else -> {}
                    }
                    swipeState.reset()
                }
                SwipeToDismissBox(
                    state = swipeState,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer(),
                    ),
                    backgroundContent = {
                        val color = when (swipeState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> Color.Red
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Surface(color = color, modifier = Modifier.fillMaxSize()) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Edit,
                                    contentDescription = "",
                                    modifier = Modifier.padding(16.dp).size(24.dp)
                                )
                            }
                        }
                    },
                    content = {
                        ListItem(
                            headlineContent = {
                                Text(text = it?.name ?: "")
                            },
                            overlineContent = {
                                Text(text = it?.teacherName ?: "")
                            },
                            supportingContent = {
                                Text(text = it?.classroomName ?: "")
                            },
                            modifier = Modifier.placeholder(
                                visible = visible,
                                highlight = PlaceholderHighlight.shimmer(),
                            )
                        )
                    },
                )
            }
        }
        return
    }
    LazyColumn {
        items(state?.data ?: List(6) { null }) {
            ListItem(
                headlineContent = {
                    Text(text = it?.name ?: "")
                },
                overlineContent = {
                    Text(text = it?.teacherName ?: "")
                },
                supportingContent = {
                    Text(text = it?.classroomName ?: "")
                },
                trailingContent = {
                    Row {
                        IconButton(
                            onClick = {
                                onCourseItemClicked(it!!)
                            },
                            enabled = it?.isUserAdded == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit"
                            )
                        }

                        IconButton(
                            onClick = {
                                onCourseItemRemoveClicked(it!!)
//                                state.removeCourse(it!!)
                            },
                            enabled = it?.isUserAdded == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }
                    }
                },
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer(),
                )
            )
        }
    }
}