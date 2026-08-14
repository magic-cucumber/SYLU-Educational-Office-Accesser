package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.OverlayClip
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareBoundsComposed

@Serializable
data object CourseListRoute

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen() = RevealContainer(2, AppInitializeMMKV::tutorialCourseList) {
    val nav = LocalNavController.current
    val mainViewModel = mainViewModelOrNull() ?: return@RevealContainer
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CourseListViewModel>(key = syncState.toViewModelKey()) {
        CourseListViewModel(syncState)
    }

    val state by model.collectAsState()
    val fabArrow = when (currentLayoutType()) {
        NavigationSuiteType.NavigationBar -> ContainerArrow.Top
        else -> ContainerArrow.Bottom
    }
    val fabShape = when (currentLayoutType()) {
        NavigationSuiteType.NavigationDrawer -> FloatingActionButtonDefaults.extendedFabShape
        else -> FloatingActionButtonDefaults.shape
    }
    HomeScreen(
        route = EOAHomeModule.COURSE,
        title = {
            when (val it = state) {
                is CourseListState.DataAccessible -> {
                    val it = it.state.currentPage

                    AnimatedContent(
                        targetState = it,
                        transitionSpec = createMenuButtonAnim { initialState > targetState }
                    ) {
                        Text(
                            text = "第 ${it + 1} 周",
                            modifier = Modifier.placeholder(
                                visible = it == -1,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                    }
                }

                else -> {
                    Text("课程表")
                }
            }
        },
        menu = {
            var iconExpanded by remember {
                mutableStateOf(false)
            }

            IconButton(
                onClick = {
                    iconExpanded = true
                },
                enabled = state is CourseListState.DataAccessible,
                modifier = Modifier.revealableAutoMeasured(0, ContainerArrow.Bottom) {
                    Text("点这里可以刷新课表、回到本周，也可以快速跳到其他周。")
                }
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                )
            }

            var jumpModal by rememberSaveable {
                mutableStateOf(false)
            }

            if (jumpModal) {
                ModalBottomSheet(
                    onDismissRequest = { jumpModal = false }
                ) {
                    val weeks = (state as? CourseListState.DataAccessible)?.allWeek ?: -1
                    if (weeks == -1) {
                        return@ModalBottomSheet
                    }
                    LazyColumn {
                        items((1..weeks).toList()) {
                            ListItem(
                                headlineContent = {
                                    Text("第 $it 周")
                                },
                                modifier = Modifier.clickable {
                                    model.selectToWeek(it)
                                    jumpModal = false
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = BottomSheetDefaults.ContainerColor
                                )
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = iconExpanded,
                onDismissRequest = {
                    iconExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("刷新")
                    },
                    onClick = {
                        model.refresh()
                        iconExpanded = false
                    }
                )

                if (state is CourseListState.Success) {
                    DropdownMenuItem(
                        text = {
                            Text("回到本周")
                        },
                        onClick = {
                            model.selectToWeek()
                            iconExpanded = false
                        }
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text("跳转到...")
                    },
                    onClick = {
                        jumpModal = true
                        iconExpanded = false
                    }
                )
            }
        },
        fabIcon = {
            Icon(
                Icons.AutoMirrored.Filled.ViewList,
                contentDescription = "ViewList",
            )
        },
        fabText = {
            Text("管理课表")
        },
        fabOnClick = {
            nav.navigate(CourseManageListRoute)
        },
        fabModifier = Modifier
            .shareBoundsComposed(
                sharedContentState = rememberSharedContentState(key = "list-course-to-manage-course"),
                animatedVisibilityScope = LocalAnimatedContentScope.current,
                resizeMode = RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(fabShape)
            )
            .revealableAutoMeasured(1, fabArrow) {
                Text("点这里管理课表，可以添加、修改课程，也可以导出课表。")
            }
    ) {
        val scope = rememberCoroutineScope()
        model.collectSideEffect {
            when (it) {
                is CourseListSideEffect.Toast -> {
                    mainViewModel.toast(type = SnackBarType.Warning, it.msg)
                }

                is CourseListSideEffect.ScrollToCurrentWeek -> {
                    // 在 UI 层执行动画，这里已经有正确的 Compose 上下文
                    scope.launch {
                        (state as? CourseListState.DataAccessible)?.state?.animateScrollToPage(it.page)
                    }
                }
            }
        }

        CourseListScreenContent(
            state = state,
        )
    }

}

@Composable
private fun CourseListScreenContent(
    state: CourseListState,
) = when (state) {
    is CourseListState.Loading -> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.width(16.dp))
                Text("正在同步课表，请稍等。")
            }
        }
    }

    is CourseListState.DataAccessible -> {
        CourseDrawerContent(state)
    }

    is CourseListState.Failed -> {
        ErrorPage(
            title = {
                Text("同步失败")
            },
            message = {
                Text(state.msg)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    is CourseListState.AfterTerm -> {
        ErrorPage(
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("温馨提示")
            },
            message = {
                Text("享受假期吧!")
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CourseDrawerContent(state: CourseListState.DataAccessible) {
    HorizontalPager(
        state = state.state,
        modifier = Modifier.fillMaxSize(),
    ) {
        CoursePageListScreen(
            index = it,
            courseListState = state,
        )
    }
}
