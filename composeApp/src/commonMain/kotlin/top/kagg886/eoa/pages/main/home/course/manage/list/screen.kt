package top.kagg886.eoa.pages.main.home.course.manage.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.AdaptiveListItem
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.course.export_calender.CourseExportCalenderRoute
import top.kagg886.eoa.pages.main.home.course.export_ics.CourseExportIcsRoute
import top.kagg886.eoa.pages.main.home.course.manage.edit.CourseEditRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.OverlayClip
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareBoundsComposed

@Serializable
data object CourseManageListRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CourseManageListScreen() = RevealContainer(3, AppInitializeMMKV::tutorialCourseManage) {
    val mainModel = mainViewModelOrNull() ?: return@RevealContainer
    val mainState by mainModel.collectAsState()
    val model = viewModel {
        CourseManageListModel(mainState, mainModel.database)
    }
    val nav = LocalNavController.current
    val state by model.collectAsState()
    val menuArrow = ContainerArrow.Bottom
    val surfaceArrow = ContainerArrow.Top
    val fabArrow = when (currentLayoutType()) {
        NavigationSuiteType.NavigationBar -> ContainerArrow.Top
        else -> ContainerArrow.Bottom
    }
    val surfaceShape = MaterialTheme.shapes.extraLarge

    HomeScreen(
        route = EOAHomeModule.COURSE,
        back = { BackIconButton() },
        title = { Text("管理课程") },
        menu = {
            var showDropdownMenu by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showDropdownMenu = true },
                enabled = state is CourseManageState.Success,
                modifier = Modifier.revealableAutoMeasured(0, menuArrow) {
                    Text("点这里可以隐藏系统课程，也可以把课表保存成文件或写入系统日历。")
                }
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
                        showDropdownMenu = false
                        model.startExportICS()
                    },
                    text = { Text("保存为ICS文件") }
                )

                DropdownMenuItem(
                    onClick = {
                        showDropdownMenu = false
                        model.startExportCalender()
                    },
                    text = { Text("写入系统日历") }
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
        fabOnClick = { model.openAddOrEditCourse(null) },
        fabModifier = Modifier.revealableAutoMeasured(2, fabArrow) {
            Text("点这里添加一门自己的课程，适合补充临时课。")
        }
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
                    nav.navigate(CourseExportIcsRoute)
                }

                is CourseManageSideEffect.StartExportCalender -> {
                    nav.navigate(CourseExportCalenderRoute)

                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shareBoundsComposed(
                    sharedContentState = rememberSharedContentState(key = "list-course-to-manage-course"),
                    animatedVisibilityScope = LocalAnimatedContentScope.current,
                    
                    clipInOverlayDuringTransition = OverlayClip(surfaceShape)
                )
                .clip(surfaceShape)
                .revealableAutoMeasured(1, surfaceArrow) {
                    Text(
                        if (currentLayoutType() == NavigationSuiteType.NavigationBar) {
                            "这里是课程管理列表。左右滑动课程，可以编辑或删除。"
                        } else {
                            "这里是课程管理列表。点击铅笔按钮编辑课程，点击垃圾桶按钮删除课程。"
                        }
                    )
                },
            shape = surfaceShape
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
    LazyColumn {
        items(state?.data ?: List(6) { null }) {
            AdaptiveListItem(
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
            ) {
                primaryAction {
                    enable = it?.isUserAdded == true
                    clickable {
                        onCourseItemClicked(it!!)
                    }
                    icon {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                }

                secondAction {
                    enable = it?.isUserAdded == true
                    clickable {
                        onCourseItemRemoveClicked(it!!)
                    }
                    icon {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
            }
        }
    }
}
