package top.kagg886.eoa.pages.main.home.course.conflict

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.util.LocalDateTimeAsLongSerializer

@Serializable
data class CourseConflictRoute(
    @Serializable(with = LocalDateTimeAsLongSerializer::class)
    val startTime: LocalDateTime,
    @Serializable(with = LocalDateTimeAsLongSerializer::class)
    val endTime: LocalDateTime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseConflictScreen(route: CourseConflictRoute) {
    val mainModel = mainViewModelOrNull() ?: return
    val model = viewModel {
        CourseConflictViewModel(
            database = mainModel.database,
            startTime = route.startTime,
            endTime = route.endTime
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
            modifier = Modifier.fillMaxHeight(0.8f)
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
                        modifier = Modifier.clip(CardDefaults.shape)
                            .clickable { onCourseItemClicked(it) },
                        colors = ListItemDefaults.colors(
                            containerColor = AlertDialogDefaults.containerColor
                        ),
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
