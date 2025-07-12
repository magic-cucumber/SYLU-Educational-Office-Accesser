package top.kagg886.eoa.pages.main.home.exam.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.ExamEntity
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
                mainViewModel.toast(type = SnackBarType.Info,it.message)
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
    val visible by remember(state) {
        derivedStateOf {
            state == null
        }
    }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分数: ${entity?.absoluteScore ?: ""}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.placeholder(
                        visible = visible,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            }

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
        val text:String,
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
            Text(
                text = "考试详情",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            DetailItem("教师", state?.teacherName ?: "", visible)
            DetailItem("学分", state?.credit?.toString() ?: "", visible)
            DetailItem("绩点", state?.gradePoint?.toString() ?: "", visible)
            DetailItem("评价", state?.relateScore ?: "", visible)
            DetailItem(
                "是否为学位课",
                if (state?.degree == true) "是" else "否",
                visible
            )
            DetailItem("学年", state?.year ?: "", visible)
            DetailItem("学期", state?.semester ?: "", visible)
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

            for (it in entity ?: List(3) {null}) {
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
