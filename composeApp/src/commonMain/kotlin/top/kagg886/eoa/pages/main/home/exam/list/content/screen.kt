package top.kagg886.eoa.pages.main.home.exam.list.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.exam.list.ExamListScreen
import top.kagg886.eoa.pages.main.home.exam.list.ExamListState
import top.kagg886.eoa.pages.main.home.exam.list.examListViewModelOrNull
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus
import top.kagg886.util.toFixed

@Serializable
data object ExamListContentRoute

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExamListContentScreen() = RevealContainer(3, AppInitializeMMKV::tutorialExamList) {
    ExamListScreen {
        val model = examListViewModelOrNull() ?: return@ExamListScreen
        val state by model.collectAsState()
        val fabArrow = when (currentLayoutType()) {
            NavigationSuiteType.NavigationBar -> ContainerArrow.Top
            else -> ContainerArrow.Bottom
        }

        HomeScreen(
            route = EOAHomeModule.EXAM,
            title = {
                Text("考试列表")
            },
            menu = {
                val scope = rememberCoroutineScope()
                var expanded by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.revealableAutoMeasured(0, ContainerArrow.Bottom) {
                        Text("点这里可以打开筛选抽屉，也可以导出当前考试数据。")
                    }
                ) {
                    AnimatedContent(
                        targetState = expanded,
                        transitionSpec = createMenuButtonAnim { expanded }
                    ) {
                        when (it) {
                            true -> Icon(
                                Icons.Default.Close,
                                contentDescription = "菜单"
                            )

                            false -> Icon(
                                Icons.Default.Menu,
                                contentDescription = "关闭"
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            model.navigateToFilter()
                            expanded = false
                        },
                        text = {
                            Text("开启筛选")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "open",
                            )
                        },
                        enabled = state is ExamListState.Success
                    )

                    DropdownMenuItem(
                        onClick = {
                            model.navigateToExport()
                            expanded = false
                        },
                        text = {
                            Text("导出")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "export"
                            )
                        },
                        enabled = state is ExamListState.Success
                    )
                }
            },
            fabIcon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null
                )
            },
            fabText = {
                Text("绩点统计")
            },
            fabOnClick = model::navigateToStatistic,
            fabModifier = Modifier.revealableAutoMeasured(2, fabArrow) {
                Text("点这里查看本地统计的总绩点。")
            }
        ) {
            ExamListScreenContent(
                state,
                modifier = Modifier.revealableAutoMeasured(1, ContainerArrow.Top) {
                    Text("这里是考试列表。点击考试可以查看更多信息。例如历史挂科，得分组成等。")
                },
                onExamItemClicked = {
                    model.navigateToDetail(it)
                },
            )
        }
    }
}


@Composable
fun ExamListScreenContent(
    state: ExamListState,
    modifier: Modifier = Modifier,
    onExamItemClicked: (ExamEntity) -> Unit = {},
) {
    when (state) {
        is ExamListState.Failed -> {
            ErrorPage(
                title = { Text("考试列表加载失败") },
                message = { Text(state.msg) },
                modifier = modifier.fillMaxSize()
            )
        }

        is ExamListState.Loading -> {
            ExamListContent(
                null,
                modifier,
                onExamItemClicked
            )
        }

        is ExamListState.Success -> {
            ExamListContent(
                state,
                modifier,
                onExamItemClicked,
            )
        }
    }
}

@Composable
fun ExamListContent(
    state: ExamListState.Success?,
    modifier: Modifier = Modifier,
    onExamItemClicked: (ExamEntity) -> Unit,
) {
    if (state?.entity?.isEmpty() == true) {
        ErrorPage(
            title = { Text("没有考试") },
            message = { Text("点击菜单按钮以弹出筛选框") },
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    val layoutType = currentLayoutType()

    val lazyListState = remember(state) {
        state?.lazyListState ?: LazyListState()
    }
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxWidth().miuiLongShotSupport(lazyListState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (layoutType == NavigationSuiteType.NavigationBar) {
            items(state?.entity ?: List(6) { null }) { exam ->
                ExamItem(
                    exam = exam,
                    modifier = Modifier.fillMaxWidth(),
                    onExamItemClicked = onExamItemClicked
                )
            }
            return@LazyColumn
        }

        items((state?.entity ?: List(6) { null }).chunked(2)) { exam ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExamItem(
                    exam = exam[0],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onExamItemClicked = onExamItemClicked
                )

                if (exam.size == 1) {
                    return@Row
                }
                Spacer(Modifier.width(16.dp))

                ExamItem(
                    exam = exam[1],
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onExamItemClicked = onExamItemClicked
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExamItem(
    exam: ExamEntity?,
    modifier: Modifier = Modifier,
    onExamItemClicked: (ExamEntity) -> Unit,
) {
    val showPlaceHolder by remember(exam) {
        derivedStateOf {
            exam == null
        }
    }

    OutlinedCard(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !showPlaceHolder) {
                exam?.let { onExamItemClicked(it) }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = exam?.name ?: "课程名称",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.placeholder(
                        visible = showPlaceHolder,
                        highlight = PlaceholderHighlight.shimmer()
                    ).shareElementComposed(
                        sharedContentState = rememberSharedContentState(key = "exam-to-detail-${exam?.id}"),
                        animatedVisibilityScope = LocalAnimatedContentScope.current
                    ),
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = "学分 × 绩点: ${
                            exam?.let {
                                "${it.credit} × ${it.gradePoint} = ${
                                    (it.credit * it.gradePoint).toFixed(
                                        2
                                    )
                                }"
                            } ?: "0.0 × 0.0 = 0.0"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.placeholder(
                            visible = showPlaceHolder,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )

                    Text(
                        text = "教师: ${exam?.teacherName ?: "未知"}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.placeholder(
                            visible = showPlaceHolder,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            },
            leadingContent = {
                Icon(
                    imageVector = when (exam?.status) {
                        ExamStatus.SUCCESS -> Icons.Default.Check
                        ExamStatus.FAILED -> Icons.Default.Close
                        ExamStatus.RE_SUCCESS -> Icons.Default.Refresh
                        null -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = when (exam?.status) {
                        ExamStatus.SUCCESS -> Color.Green
                        ExamStatus.FAILED -> Color.Red
                        ExamStatus.RE_SUCCESS -> Color.Blue
                        null -> Color.Green
                    },
                    modifier = Modifier.placeholder(
                        visible = showPlaceHolder,
                        highlight = PlaceholderHighlight.shimmer()
                    ),
                )
            },
            trailingContent = {
                if (showPlaceHolder || exam?.degree == true) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "学位课程",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.placeholder(
                            visible = showPlaceHolder,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }
        )
    }
}
