package top.kagg886.eoa.pages.main.home.summary

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.home.course.detail.CourseDetailRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed

@Serializable
data object SummaryRoute

@Composable
fun SummaryScreen() = HomeScreen(
    route = NavigationRoute.SUMMARY,
    title = {
        Text("概要")
    }
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<SummaryModel>(key = syncState.toString()) {
        SummaryModel(syncState, mainViewModel.database)
    }
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is SummarySideEffect.NavigateToCourseInfo -> {
                nav.navigate(CourseDetailRoute(it.courseId))
            }
        }
    }
    val state by model.collectAsState()

    SummaryContent(
        state = state,
        onCourseItemClicked = { model.redirectToCourse(it) },
    )
}

@Composable
private fun SummaryContent(
    state: SummaryState,
    onCourseItemClicked: (TodayClass) -> Unit = {},
) {
    when (state) {
        is SummaryState.Loading -> {
            SummaryContentPlaceHolder(null)
        }

        is SummaryState.Success -> {
            SummaryContentPlaceHolder(
                state = state,
                onCourseItemClicked = onCourseItemClicked
            )
        }

        is SummaryState.Failed -> {
            // 加载失败的页面
            ErrorPage(
                title = {
                    Text("数据同步失败")
                },
                message = {
                    Text("详细信息请检查日志")
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        is SummaryState.FailedButSuccess -> {
            //特殊情况下的提示(如正在假期)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "提示",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.msg,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    state: SummaryState.Success?,
    showPlaceHolder: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Current week indicator
                SummaryItem(
                    showPlaceHolder = showPlaceHolder,
                    details = "第 ${state?.weekNumber} 周",
                    title = "当前周数",
                )
                // Today's classes count
                SummaryItem(
                    showPlaceHolder = showPlaceHolder,
                    details = "${state?.plan?.size ?: 0} 门",
                    title = "今日课程数",
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
private fun SummaryContentPlaceHolder(
    state: SummaryState.Success?,
    onCourseItemClicked: (TodayClass) -> Unit = {},
) {
    val showPlaceHolder by remember(state) {
        derivedStateOf {
            state == null
        }
    }

    if (state?.plan?.isEmpty() == true) {
        Column {
            SummaryCard(state, showPlaceHolder)
            Box(Modifier.weight(1f),  contentAlignment = Alignment.Center) {
                Text(
                    text = "今天没有课程安排",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SummaryCard(state, showPlaceHolder)
        }

        item {
            Text(
                text = "今日课程",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.placeholder(
                    visible = showPlaceHolder,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        }


        val items = state?.plan ?: List(3) { null }
        items(items) { course ->
            CourseItem(course, onCourseItemClicked)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CourseItem(
    course: TodayClass?,
    onCourseItemClicked: (TodayClass) -> Unit
) {
    val showPlaceHolder by remember {
        derivedStateOf {
            course == null
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shareElementComposed(
                sharedContentState = rememberSharedContentState(key = "summary-course-to-detail-${course?.recordId}"),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            )
            .clickable(onClick = { course?.let { onCourseItemClicked(it) } }),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Progress bar for ongoing classes
            course?.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = course?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                    
                    // Show an indicator for ongoing classes
                    course?.progress?.let {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "进行中",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )

                    Text(
                        text = course?.date?.let { (start, end) -> "$start - $end" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                    
                    Text(
                        text = course?.location ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .placeholder(
                                visible = showPlaceHolder,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "授课教师: ${course?.teacher ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.placeholder(
                        visible = showPlaceHolder,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    showPlaceHolder: Boolean,
    details: String,
    title: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(width = 80.dp, height = 48.dp)
                .placeholder(
                    visible = showPlaceHolder,
                    highlight = PlaceholderHighlight.shimmer()
                ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.placeholder(
                visible = showPlaceHolder,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}