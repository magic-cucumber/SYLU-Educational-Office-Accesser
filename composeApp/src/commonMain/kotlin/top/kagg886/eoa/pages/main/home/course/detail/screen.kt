package top.kagg886.eoa.pages.main.home.course.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
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
import top.kagg886.util.toFixed

@Serializable
data class CourseDetailRoute(val recordId: Long)


@Composable
fun CourseDetailScreen(route: CourseDetailRoute) = HomeScreen(
    EOAHomeModule.COURSE,
    title = { Text("课程详情") },
    back = {
        BackIconButton()
    }
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<CourseDetailViewModel>(key = syncState.toString()) {
        CourseDetailViewModel(route.recordId, syncState, mainViewModel.database)
    }
    val state by model.collectAsState()

    model.collectSideEffect {
        when (it) {
            is CourseDetailSideEffect.ShowToast -> {
                mainViewModel.toast(type = SnackBarType.Info, it.message)
            }
        }
    }

    CourseDetailScreenContent(state, route.recordId)
}

@Composable
private fun CourseDetailScreenContent(state: CourseDetailState, recordId: Long) {
    when (state) {
        is CourseDetailState.Failed -> {
            ErrorPage(
                title = { Text("课程加载失败") },
                message = { Text(state.msg) },
            )
        }

        CourseDetailState.Loading -> {
            CourseDetailScreenSuccess(null, recordId)
        }

        is CourseDetailState.Success -> {
            CourseDetailScreenSuccess(state, recordId)
        }
    }
}

@Composable
private fun CourseDetailScreenSuccess(
    state: CourseDetailState.Success?,
    recordId: Long
) {
    val design = currentLayoutType()
    when (design) {
        NavigationSuiteType.NavigationBar -> {
            CourseDetailPanelPhone(state, recordId)
        }

        NavigationSuiteType.NavigationRail -> {
            CourseDetailPanelTablet(state, recordId)
        }

        NavigationSuiteType.NavigationDrawer -> {
            CourseDetailPanelTablet(state, recordId)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CourseDetailPanelPhone(
    state: CourseDetailState.Success?,
    recordId: Long
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
        // 课程卡片
        CourseCard(
            state, visible, Modifier.shareElementComposed(
                sharedContentState = rememberSharedContentState(key = "summary-course-to-detail-$recordId"),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            ).shareElementComposed(
                sharedContentState = rememberSharedContentState(key = "list-course-to-detail-$recordId"),
                animatedVisibilityScope = LocalAnimatedContentScope.current
            )
        )

        // 课程详情
        CourseDetails(state, visible)

        // 上课时间
        ClassSchedule(state, visible)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CourseDetailPanelTablet(
    state: CourseDetailState.Success?,
    recordId: Long
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
        // 课程卡片与课程详情排一行
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 课程卡片
            CourseCard(
                state,
                visible,
                modifier = Modifier.weight(1f).fillMaxHeight().shareElementComposed(
                    sharedContentState = rememberSharedContentState(key = "summary-course-to-detail-$recordId"),
                    animatedVisibilityScope = LocalAnimatedContentScope.current
                ).shareElementComposed(
                    sharedContentState = rememberSharedContentState(key = "list-course-to-detail-$recordId"),
                    animatedVisibilityScope = LocalAnimatedContentScope.current
                )
            )

            // 课程详情
            CourseDetails(
                state,
                visible,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // 上课时间
        ClassSchedule(state, visible)
    }
}

@Composable
private fun CourseCard(
    state: CourseDetailState.Success?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state?.entity?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 课程类型badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            ) {
                AssistChip(
                    enabled = state?.entity?.isDegreeRequired == true,
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("学位课") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        trailingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )

                AssistChip(
                    enabled = state?.entity?.isExaminable == true,
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    label = { Text("考试课") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))


            AssistChip(
                enabled = false,
                onClick = {},
                shape = RoundedCornerShape(8.dp),
                label = {
                    Text("课程进度： ${((state?.progress ?: 0f) * 100).toFixed(2)}%")
                },
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                ),
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = AssistChipDefaults.assistChipColors().containerColor,
                    disabledLabelColor = AssistChipDefaults.assistChipColors().labelColor,
                    disabledLeadingIconContentColor = AssistChipDefaults.assistChipColors().leadingIconContentColor,
                    disabledTrailingIconContentColor = AssistChipDefaults.assistChipColors().trailingIconContentColor,
                )
            )
        }
    }
}

@Composable
private fun CourseDetails(
    state: CourseDetailState.Success?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
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
                text = "课程详情",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            DetailItem("教师", state?.entity?.teacherName ?: "", visible)
            DetailItem("学分", state?.entity?.credits?.toString() ?: "", visible)
            DetailItem("自定义课程",  if (state?.entity?.isUserAdded == true) "是" else "否", visible)

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
private fun ClassSchedule(
    state: CourseDetailState.Success?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "上课时间",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.placeholder(
                visible = visible,
                highlight = PlaceholderHighlight.shimmer()
            )
        )

        if (currentLayoutType() != NavigationSuiteType.NavigationBar) {
            for (record in (state?.records ?: List(6) { null }).chunked(2)) {
                Row(
                    modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClassUnitData(
                        modifier = Modifier.weight(1f),
                        record = record[0],
                        visible = visible
                    )

                    if (record.size == 1) {
                        return@Row
                    }

                    ClassUnitData(
                        modifier = Modifier.weight(1f),
                        record = record[1],
                        visible = visible
                    )
                }
            }
            return@Column
        }

        for (record in state?.records ?: List(3) { null }) {
            ClassUnitData(
                modifier, record, visible
            )
        }
    }
}

@Composable
private fun ClassUnitData(
    modifier: Modifier = Modifier,
    record: CourseRecordAndProgress?,
    visible: Boolean
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            record?.progressStatus?.let {
                Icon(
                    imageVector = when (it) {
                        CourseRecordAndProgress.ProgressStatus.NotStarted -> Icons.Default.Pending
                        CourseRecordAndProgress.ProgressStatus.InProgress -> Icons.Default.Route
                        CourseRecordAndProgress.ProgressStatus.Completed -> Icons.Default.TaskAlt
                    },
                    contentDescription = ""
                )
            }
        },
        headlineContent = {
            Text(
                record?.date.toString(),
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        },
        supportingContent = {
            Text(
                "${record?.start.toString()} - ${record?.start.toString()}",
                modifier = Modifier.placeholder(
                    visible = visible,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        }
    )
}
