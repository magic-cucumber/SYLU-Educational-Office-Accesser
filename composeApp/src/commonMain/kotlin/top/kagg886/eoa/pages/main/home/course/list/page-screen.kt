package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
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
    val model = viewModel<CoursePageViewModel>(key = "${index * 31 + syncState.hashCode()}") {
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("正在加载课表，请稍等。")
                }
            }
        }

        is CoursePageState.Success -> {
            CoursePageScreenSuccess(
                state = state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CoursePageScreenSuccess(
    state: CoursePageState.Success,
    modifier: Modifier = Modifier
) {
    Row(modifier.verticalScroll(rememberScrollState())) {
        for (i in 1..7) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.thisWeekStartDate.plus(i - 1, DateTimeUnit.DAY)
                        .format(OnlyMonthAndDayFormat),
                    modifier = Modifier.height(cardHeight / 2).align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider()

                Box(modifier = Modifier.height(columnHeight)) {
                    for ((next, course) in state.currentWeekCourse[i].orEmpty()) {
                        val topOffset = (cardHeight + verticalSpacing) * (next - 1) // 节次从1开始

                        Card(
                            modifier = Modifier
                                .offset(y = topOffset)
                                .fillMaxWidth()
                                .height(cardHeight)
                        ) {
                            if (course.hasConflict) {
                                Text("冲突课程")
                                return@Card
                            }
                            Text(course.asNoConflict.course.name)
                        }
                    }
                }
            }
        }
    }
}

private val OnlyMonthAndDayFormat = LocalDate.Format {
    monthNumber()
    char('-')
    dayOfMonth()
}

val cardHeight = 60.dp  // 每节课高度（你可以根据 UI 修改）
val verticalSpacing = 8.dp  // 每节课之间的空隙
val columnHeight = 12 * (cardHeight + verticalSpacing) // 课程表总高度
