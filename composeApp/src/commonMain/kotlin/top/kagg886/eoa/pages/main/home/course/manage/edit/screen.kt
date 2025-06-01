package top.kagg886.eoa.pages.main.home.course.manage.edit

import StackedSnackbarHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import rememberStackedSnackbarHostState
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.showSnackBar

//新增为null，否则为id
@Serializable
data class CourseEditRoute(
    val id: Long? = null
)

@Composable
fun CourseEditScreen(route: CourseEditRoute) {
    val mainModel = mainViewModel()
    val model = viewModel {
        CourseEditModel(mainModel.database, route.id)
    }

    val state by model.collectAsState()
    val snack = rememberStackedSnackbarHostState(animation = StackedSnackbarAnimation.Slide)
    model.collectSideEffect {
        when (it) {
            is CourseEditSideEffect.Toast -> snack.showSnackBar(it.type, it.message)
        }
    }
    Box(Modifier.fillMaxSize(),contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxSize(0.8f)) {
            CompositionLocalProvider(
                LocalSnackBarHost provides snack,
            ) {
                CourseEditScreenContent(
                    state = state,
                    onCourseModified = { model.modifyCourse(it) },
                    onCourseInfoConfirmed = { model.confirmModifyCourse() }
                )
            }
        }

        StackedSnackbarHost(
            hostState = snack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditScreenContent(
    state: CourseEditState,
    onCourseModified: (CourseEntity) -> Unit,
    onCourseInfoConfirmed: () -> Unit
) {
    when (state) {
        is CourseEditState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("正在加载中，请稍等。")
                }
            }
        }

        is CourseEditState.Success -> {
            val pagerState = rememberPagerState(0) { 2 }
            val scope = rememberCoroutineScope()
            Column {
                TopAppBar(
                    title = {
                        Text("编辑课程")
                    }
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    tabs = {
                        Tab(
                            text = { Text("课程信息") },
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                        )
                        Tab(
                            text = { Text("时间编辑") },
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                        )
                    }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) {
                    when (it) {
                        0 -> CourseEditBasic(
                            course = state.courseInfo,
                            onCourseModified = onCourseModified
                        )

                        1 -> CourseEditTime()
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onCourseInfoConfirmed,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseEditBasic(
    course: CourseEntity,
    onCourseModified: (CourseEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = course.name,
            onValueChange = {
                onCourseModified(course.copy(name = it))
            },
            label = { Text("课程名称") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        OutlinedTextField(
            value = course.teacherName,
            onValueChange = {
                onCourseModified(course.copy(teacherName = it))
            },
            label = { Text("教师姓名") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = course.classroomName,
            onValueChange = {
                onCourseModified(course.copy(classroomName = it))
            },
            label = { Text("教室名称") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = course.credits.toString(),
            onValueChange = {
                val parsedCredits = it.toFloatOrNull() ?: course.credits
                onCourseModified(course.copy(credits = parsedCredits))
            },
            label = { Text("学分") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked = course.isDegreeRequired,
                onCheckedChange = {
                    onCourseModified(course.copy(isDegreeRequired = it))
                }
            )
            Text("学位课")
        }
    }
}

@Composable
private fun CourseEditTime(

) {

}