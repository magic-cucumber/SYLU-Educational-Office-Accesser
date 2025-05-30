package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.mainViewModel

@Composable
fun CoursePageListScreen(
    index: Int
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CoursePageViewModel>(key = syncState.toString()) {
        CoursePageViewModel(syncState, index + 1, mainViewModel.database)
    }
    model.collectSideEffect {
    }
    val state by model.collectAsState()

    CoursePageScreenContent(
        state = state,
    )
}
@Composable
private fun CoursePageScreenContent(
    state: CoursePageState
) {
    when (state) {
        is CoursePageState.Failed -> {
            ErrorPage(
                title = {
                    Text(text = "加载课表失败")
                },
                message = {
                    Text(text = "请参阅系统日志")
                }
            )
        }

        is CoursePageState.Loading -> {
            CoursePageScreenSuccess(
                state = null
            )
        }

        is CoursePageState.Success -> {
            CoursePageScreenSuccess(
                state = state
            )
        }
    }
}

@Composable
private fun CoursePageScreenSuccess(
    state: CoursePageState.Success?
) {
    //TODO 课表的container内容
}