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
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseAndRecord
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import kotlin.time.Instant

@Serializable
data class CourseConflictRoute(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {
    companion object {
        val Type: NavType<LocalDateTime> = object : NavType<LocalDateTime>(false) {
            override fun put(bundle: SavedState, key: String, value: LocalDateTime) {
                bundle.write {
                    putLong(
                        key,
                        value.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    )
                }
            }

            override fun get(
                bundle: SavedState,
                key: String
            ): LocalDateTime? {
                return bundle.read {
                    try {
                        Instant.fromEpochMilliseconds(getLong(key))
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    } catch (e: Throwable) {
                        null
                    }
                }
            }

            override fun parseValue(value: String): LocalDateTime {
                return try {
                    Instant.fromEpochMilliseconds(value.toLong())
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                } catch (e: Throwable) {
                    throw IllegalArgumentException(e)
                }
            }

            override fun serializeAsValue(value: LocalDateTime): String {
                return value.toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds()
                    .toString()
            }
        }
    }
}

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
