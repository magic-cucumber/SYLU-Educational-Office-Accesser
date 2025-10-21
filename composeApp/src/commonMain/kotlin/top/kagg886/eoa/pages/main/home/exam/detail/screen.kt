package top.kagg886.eoa.pages.main.home.exam.detail

import ai.koog.prompt.text.text
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus
import top.kagg886.util.toFixed

@Serializable
data class ExamDetailRoute(val examId: Long)

//.shareElementComposed(
//     sharedContentState = rememberSharedContentState(key = "exam-to-detail-${exam?.id}"),
//     animatedVisibilityScope = LocalAnimatedContentScope.current
//)
@Composable
fun ExamDetailScreen(route: ExamDetailRoute) = HomeScreen(
    route = EOAHomeModule.EXAM,
    title = { Text("考试详情") },
    menu = null,
    back = { BackIconButton() }
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<ExamDetailViewModel>(key = syncState.toString()) {
        ExamDetailViewModel(route.examId, syncState, mainViewModel.database)
    }
    val state by model.collectAsState()

    model.collectSideEffect {
        when (it) {
            is ExamDetailSideEffect.ShowToast -> {
                mainViewModel.toast(type = SnackBarType.Info, it.message)
            }
        }
    }

    ExamDetailScreenContent(state)
}

@Composable
private fun ExamDetailScreenContent(state: ExamDetailState) {
    when (state) {
        is ExamDetailState.Failed -> {
            ErrorPage(
                title = { Text("考试详情加载失败") },
                message = { Text(state.msg) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        is ExamDetailState.Loading -> {
            ExamDetailScreenSuccess(null)
        }

        is ExamDetailState.Success -> {
            ExamDetailScreenSuccess(state)
        }
    }
}

@Composable
private fun ExamDetailScreenSuccess(
    state: ExamDetailState.Success?,
) {
    val design = currentLayoutType()
    when (design) {
        NavigationSuiteType.NavigationBar -> {
            ExamDetailPanelPhone(state)
        }

        NavigationSuiteType.NavigationRail -> {
            ExamDetailPanelTablet(state)
        }

        NavigationSuiteType.NavigationDrawer -> {
            ExamDetailPanelTablet(state)
        }
    }
}

@Composable
private fun ExamDetailPanelPhone(
    state: ExamDetailState.Success?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 分数卡片
        ScoreCard(state?.records)

        // 考试基本信息
        ExamDetails(state?.records)

        // 考试详细表格
        ExamDetailTable(state?.records?.detail)

        ExamDetailTimeLine(state?.timeline)
    }
}

@Composable
private fun ExamDetailPanelTablet(
    state: ExamDetailState.Success?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 分数卡片与考试信息排一行
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 分数卡片
            ScoreCard(
                state?.records,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            // 考试基本信息
            ExamDetails(
                state?.records,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // 考试详细表格
        ExamDetailTable(state?.records?.detail)

        ExamDetailTimeLine(state?.timeline)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ScoreCard(
    entity: ExamEntity?,
    modifier: Modifier = Modifier
) {
    val visible by remember(entity) {
        derivedStateOf {
            entity == null
        }
    }
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .placeholder(
                visible = visible,
                highlight = PlaceholderHighlight.shimmer()
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entity?.degree == true) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "学位课程",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.placeholder(
                            visible = visible,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }

                Text(
                    text = entity?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    ).shareElementComposed(
                        sharedContentState = rememberSharedContentState(key = "exam-to-detail-${entity?.id}"),
                        animatedVisibilityScope = LocalAnimatedContentScope.current
                    )
                )

            }

            Text(
                text = "分数: ${entity?.absoluteScore ?: ""}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            ExamStatusIndicator(
                status = entity?.status,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        }
    }
}

@Composable
private fun ExamStatusIndicator(
    status: ExamStatus?,
    modifier: Modifier = Modifier
) {

    data class Pair(
        val background: Color,
        val borderColor: Color,
        val text: String,
        val textColor: Color,
    )

    val (backgroundColor, borderColor, text, textColor) = when (status) {
        ExamStatus.SUCCESS -> {
            Pair(
                Color(0xFFE8F5E9).copy(alpha = 0.7f),
                Color(0xFF4CAF50),
                "通过",
                Color(0xFF2E7D32)
            )
        }

        ExamStatus.FAILED -> {
            Pair(
                Color(0xFFFFEBEE).copy(alpha = 0.7f),
                Color(0xFFE57373),
                "挂科",
                Color(0xFFD32F2F)
            )
        }

        ExamStatus.RE_SUCCESS -> {
            Pair(
                Color(0xFFFFF8E1).copy(alpha = 0.7f),
                Color(0xFFFFA726),
                "重修通过",
                Color(0xFFEF6C00)
            )
        }

        else -> {
            Pair(
                Color(0xFFE0E0E0),
                Color(0xFF9E9E9E),
                "状态未知",
                Color(0xFF616161)
            )
        }
    }

    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun ExamDetails(
    state: ExamEntity?,
    modifier: Modifier = Modifier
) {
    val visible by remember(state) {
        derivedStateOf {
            state == null
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .placeholder(
                visible = visible,
                highlight = PlaceholderHighlight.shimmer()
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
                Text(
                    text = "考试详情",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                val theme = MaterialTheme.colorScheme
                var expanded by remember {
                    mutableStateOf(false)
                }
                Text(
                    text = buildAnnotatedString {
                        withLink(
                            link = LinkAnnotation.Clickable(
                                tag = "detail",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = theme.primary,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    pressedStyle = SpanStyle(
                                        color = theme.primary.copy(alpha = 0.8f),
                                        textDecoration = TextDecoration.Underline
                                    )
                                ),
                                linkInteractionListener = { expanded = true }
                            )
                        ) {
                            append("更多")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )

                if (expanded) {
                    AlertDialog(
                        onDismissRequest = { expanded = false },
                        title = { Text("详细信息") },
                        confirmButton = { Button(onClick = { expanded = false }) { Text("确定") } },
                        text = {
                            Column {
                                DetailItem(label = "学年代号",  value = state?.year ?: "未知",false)
                                DetailItem(label = "学期代号",  value = state?.semester ?: "未知",false)
                                DetailItem(label = "课程名",  value = state?.name ?: "未知",false)
                                DetailItem(label = "教师名",  value = state?.teacherName ?: "未知",false)
                                DetailItem(label = "学分",  value = state?.credit?.toString() ?: "",false)
                                DetailItem(label = "绩点",  value = state?.gradePoint?.toString() ?: "",false)
                                DetailItem(label = "评分",  value = state?.absoluteScore ?: "",false)
                                DetailItem(label = "评价",  value = state?.relateScore ?: "",false)
                                DetailItem(label = "状态",  value = state?.status?.name ?: "",false)
                                DetailItem(label = "是否学位课",  value = state?.degree?.toString() ?: "",false)
                                DetailItem(label = "成绩提交人",  value = state?.submitTeacherName ?: "",false)
                                DetailItem(label = "提交时间",  value = state?.submitTime?.toString() ?: "",false)
                            }
                        },
                    )
                }
            }

            DetailItem("考试学制","${state?.year}学年 ${state?.semester}学期",visible)
            DetailItem("教师", state?.teacherName ?: "", visible)
            DetailItem("学分 x 绩点", if (state !== null) "${state.credit.toFixed(2)} x ${state.gradePoint.toFixed(2)} = ${(state.credit * state.gradePoint).toFixed(2)}" else "", visible)
            DetailItem("评价", state?.relateScore ?: "", visible)
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    visible: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.placeholder(
                visible = visible,
                highlight = PlaceholderHighlight.shimmer()
            )
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.placeholder(
                visible = visible,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}

@Composable
private fun ExamDetailTable(
    entity: List<List<String>>?,
    modifier: Modifier = Modifier
) {
    val visible by remember(entity) {
        derivedStateOf {
            entity == null
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            for (it in entity ?: List(3) { null }) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = it?.get(0) ?: "加载中",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.placeholder(
                                visible = visible,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                    },
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = CardDefaults.cardColors().containerColor
                    )
                )
            }
        }
    }
}

@Composable
private fun ExamDetailTimeLine(
    entity: List<ExamEntity>?,
    modifier: Modifier = Modifier
) {
    val visible by remember(entity) {
        derivedStateOf {
            entity == null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .placeholder(
                visible = visible,
                highlight = PlaceholderHighlight.shimmer()
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "考试历程",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            Spacer(Modifier.height(16.dp))

            if (entity.isNullOrEmpty()) {
                // 加载状态或无数据
                repeat(3) {
                    TimeLineItem(
                        exam = null,
                        isFirst = it == 0,
                        isLast = it == 2,
                        visible = visible
                    )
                }
            } else {
                // 显示实际数据
                entity.forEachIndexed { index, exam ->
                    TimeLineItem(
                        exam = exam,
                        isFirst = index == 0,
                        isLast = index == entity.size - 1,
                        visible = false
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeLineItem(
    exam: ExamEntity?,
    isFirst: Boolean,
    isLast: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val nav = LocalNavController.current

    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 时间线指示器
        Box(modifier = Modifier.width(24.dp).fillMaxHeight()) {
            // 上半部分连接线
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.5f)
                        .align(Alignment.TopCenter)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
            }

            // 下半部分连接线
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
            }

            // 圆点指示器（居中显示）
            val (dotColor, dotSize) = when (exam?.status) {
                ExamStatus.SUCCESS -> Color(0xFF4CAF50) to 16.dp
                ExamStatus.FAILED -> Color(0xFFE57373) to 16.dp
                ExamStatus.RE_SUCCESS -> Color(0xFFFFA726) to 16.dp
                else -> MaterialTheme.colorScheme.outline to 12.dp
            }

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(dotColor)
                    .align(Alignment.Center)
                    .placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
        }

        // 考试信息ListItem
        ListItem(
            headlineContent = {
                Text(
                    text = if (exam != null) {
                        "${exam.absoluteScore} ( ${exam.credit}x${exam.gradePoint}=${(exam.credit * exam.gradePoint).toFixed(2)} )"
                    } else {
                        "加载中"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            },
            supportingContent = {
                Text(
                    text = exam?.submitTime?.toString()?.substring(0, 16) ?: "加载中",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "查看详情",
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !visible && exam != null) {
                    exam?.id?.let { examId ->
                        nav.navigate(ExamDetailRoute(examId))
                    }
                }
                .placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                ),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
}
