package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.home.course.manage.list.CourseManageListRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed

@Serializable
data object CourseListRoute

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen() {
    val nav = LocalNavController.current
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CourseListViewModel>(key = syncState.toString()) {
        CourseListViewModel(syncState)
    }

    val state by model.collectAsState()
    HomeScreen(
        route = NavigationRoute.COURSE,
        title = {
            AnimatedContent(
                targetState = (state as? CourseListState.Success)?.state?.currentPage ?: -1,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    }
                }
            ) {
                Text(
                    text = "第 ${it + 1} 周",
                    modifier = Modifier.placeholder(
                        visible = it == -1,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
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
                enabled = state is CourseListState.Success
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
                    val weeks = (state as? CourseListState.Success)?.allWeek ?: -1
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
                                }
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
                DropdownMenuItem(
                    text = {
                        Text("回到本周")
                    },
                    onClick = {
                        model.selectToWeek()
                        iconExpanded = false
                    }
                )

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
        fabModifier = Modifier.shareElementComposed(
            sharedContentState = rememberSharedContentState(key = "list-course-to-manage-course"),
            animatedVisibilityScope = LocalAnimatedContentScope.current
        )
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
                        (state as? CourseListState.Success)?.state?.animateScrollToPage(it.page)
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

    is CourseListState.Success -> {
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

    is CourseListState.FailedButSuccess -> {
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
                Text(state.msg)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CourseDrawerContent(
    state: CourseListState.Success,
) {
    HorizontalPager(
        state = state.state,
        modifier = Modifier.fillMaxSize(),
    ) {
        CoursePageListScreen(
            index = it
        )
    }
}