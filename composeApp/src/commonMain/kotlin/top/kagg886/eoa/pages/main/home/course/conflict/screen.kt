package top.kagg886.eoa.pages.main.home.course.conflict

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.mainViewModel

@Serializable
data class CourseConflictRoute(val weekNumber: Int, val dayOfWeek: Int, val periodOfDay: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseConflictScreen(route: CourseConflictRoute) {
    val mainModel = mainViewModel()
    val model = viewModel {
        CourseConflictViewModel(
            database = mainModel.database,
            weekNumber = route.weekNumber,
            dayOfWeek = route.dayOfWeek,
            periodOfDay = route.periodOfDay
        )
    }
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is CourseConflictSideEffect.NavigateToDetail -> {
                nav.navigate(CourseDetailRoute(it.recordId))
            }
        }
    }
    val state by model.collectAsState()

    DialogPageScaffold(
        modifier = Modifier.fillMaxSize(),
        title = { Text(text = "课程冲突") },
        confirmButton = {
            TextButton(onClick = { nav.popBackStack() }) {
                Text(text = "关闭")
            }
        }
    ) {
        CourseConflictScreenContent(
            state = state,
            onCourseItemClicked = {
                model.navigateTo(it.record)
            },
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
        )
    }
}

@Composable
private fun CourseConflictScreenContent(
    state: CourseConflictState,
    onCourseItemClicked: (CourseAndRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is CourseConflictState.Success -> {
            LazyColumn(modifier) {
                items(state.course) {
                    ListItem(
                        modifier = Modifier.clip(CardDefaults.shape).clickable { onCourseItemClicked(it) },
                        headlineContent = {
                            Text(text = it.course.name)
                        },
                        supportingContent = {
                            Text(text = it.course.classroomName)
                        },
                        overlineContent = {
                            Text(text = it.course.teacherName)
                        },
                    )
                }
            }
        }

        else -> {}
    }
}
