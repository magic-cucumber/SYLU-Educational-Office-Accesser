package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.mainViewModel

@Serializable
data object CourseListRoute

@Composable
fun CourseListScreen() = HomeScreen(
    route = NavigationRoute.COURSE,
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CourseListViewModel>(key = syncState.toString()) {
        CourseListViewModel(syncState)
    }
    model.collectSideEffect {
    }
    val state by model.collectAsState()

    CourseListScreenContent(
        state = state,
    )
}

@Composable
private fun CourseListScreenContent(
    state: CourseListState,
) = when (state) {
    is CourseListState.Loading -> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
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
                Text("请查阅系统日志")
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
    //TODO 或者选择器写在这里，一个大Tab
    HorizontalPager(
        state = state.state,
        modifier = Modifier.fillMaxSize(),
    ) {
        CoursePageListScreen(
            index = it
        )
    }
}