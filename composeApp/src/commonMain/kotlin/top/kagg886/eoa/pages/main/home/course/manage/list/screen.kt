package top.kagg886.eoa.pages.main.home.course.manage.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.home.course.manage.CourseManageState
import top.kagg886.eoa.pages.main.home.course.manage.courseManageModel
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed

@Serializable
data object CourseManageListRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CourseManageListScreen() = HomeScreen(
    route = NavigationRoute.COURSE,
    back = { BackIconButton() },
    title = { Text("管理课程") },
    menu = {
        val mainModel = mainViewModel()
        val mainState by mainModel.collectAsState()
        val model = courseManageModel(mainState)
        val state by model.collectAsState()

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
        }
    }
) {

    val mainModel = mainViewModel()
    val mainState by mainModel.collectAsState()
    val model = courseManageModel(mainState)

    model.collectSideEffect {

    }
    val state by model.collectAsState()
    Surface(
        Modifier.fillMaxSize().shareElementComposed(
            sharedContentState = rememberSharedContentState(key = "list-course-to-manage-course"),
            animatedVisibilityScope = LocalAnimatedContentScope.current
        )
    ) {
        CoursePageScreenContent(state)
    }
}

@Composable
private fun CoursePageScreenContent(
    state: CourseManageState,
): Unit = when (state) {
    CourseManageState.Failed -> {
        ErrorPage(
            title = { Text("获取课表失败") },
            message = { Text("检查日志后重试") },
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

    CourseManageState.Loading -> CoursePageScreenSuccessContent(null)
    is CourseManageState.Success -> CoursePageScreenSuccessContent(state)
}

@Composable
private fun CoursePageScreenSuccessContent(
    state: CourseManageState.Success?,
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
        }
    }
}