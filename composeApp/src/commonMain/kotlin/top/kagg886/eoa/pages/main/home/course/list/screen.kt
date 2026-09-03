package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
    val dataAccessibleState = state as? CourseListState.DataAccessible
    val pagerState = dataAccessibleState?.state
    val currentPage = pagerState?.currentPage
    val allWeek = dataAccessibleState?.allWeek
    val isCurrentTerm = state is CourseListState.Success
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
            if (currentPage != null) {
                AnimatedContent(
                    targetState = currentPage,
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
            } else {
                Text("课程表")
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
                enabled = pagerState != null,
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
                    val weeks = allWeek ?: -1
                    if (weeks == -1) {
                        return@ModalBottomSheet
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 48.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items((1..weeks).toList()) { week ->
                            val isCurrent = currentPage != null && currentPage + 1 == week
                            Surface(
                                onClick = {
                                    model.selectToWeek(week - 1)
                                    jumpModal = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                shape = MaterialTheme.shapes.medium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                                contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                border = if (isCurrent) null
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "$week",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
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

                if (isCurrentTerm) {
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
                        pagerState?.animateScrollToPage(it.page)
                    }
                }
            }
        }

        CourseListScreenContent(
            state = state,
            onZoomChange = model::scaleBy,
        )
    }

}

@Composable
private fun CourseListScreenContent(
    state: CourseListState,
    onZoomChange: (Float) -> Unit,
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
        val scale by state.scale.collectAsState()

        CourseDrawerContent(
            pagerState = state.state,
            scale = scale,
            onZoomChange = onZoomChange,
        )
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
private fun CourseDrawerContent(
    pagerState: PagerState,
    scale: Float,
    onZoomChange: (Float) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) {
        CoursePageListScreen(
            index = it,
            isCurrentPage = pagerState.currentPage == it,
            scale = scale,
            onZoomChange = onZoomChange,
        )
    }
}
