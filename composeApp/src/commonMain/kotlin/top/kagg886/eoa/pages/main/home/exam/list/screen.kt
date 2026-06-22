package top.kagg886.eoa.pages.main.home.exam.list

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.drawer.SupportRTLModalNavigationDrawer
import top.kagg886.eoa.component.reveal.ContainerArrow
import top.kagg886.eoa.component.reveal.RevealContainer
import top.kagg886.eoa.component.reveal.revealableAutoMeasured
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailRoute
import top.kagg886.eoa.pages.main.home.exam.export.ExamExportRoute
import top.kagg886.eoa.pages.main.home.exam.statistic.ExamStatisticRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.longshot.miuiLongShotSupport
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed
import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus
import top.kagg886.util.toFixed

@Serializable
data object ExamListRoute

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExamListScreen() = RevealContainer(3, AppInitializeMMKV::tutorialExamList) {
    val mainViewModel = mainViewModelOrNull() ?: return@RevealContainer
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<ExamListViewModel>(key = syncState.toViewModelKey()) {
        ExamListViewModel(mainViewModel.database, syncState)
    }
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is ExamListSideEffect.NavigateToDetail -> {
                nav.navigate(ExamDetailRoute(it.examId))
            }

            is ExamListSideEffect.NavigateToStatistic -> {
                nav.navigate(ExamStatisticRoute(it.year, it.term))
            }

            is ExamListSideEffect.NavigateToExport -> {
                nav.navigate(ExamExportRoute(it.year,it.term))
            }
        }
    }
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
            val mainViewModel = mainViewModelOrNull() ?: return@HomeScreen
            val syncState by mainViewModel.collectAsState()
            val model = viewModel<ExamListViewModel>(key = syncState.toViewModelKey()) {
                ExamListViewModel(mainViewModel.database, syncState)
            }
            val state by model.collectAsState()
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
                        scope.launch {
                            if (state.drawerState.isOpen) { state.drawerState.close() } else { state.drawerState.open() }
                        }
                        expanded = false
                    },
                    text = {
                        Text("${if (state.drawerState.isOpen) "关闭" else "开启"}筛选")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.drawerState.isOpen) Icons.Default.Close else Icons.Default.FilterList,
                            contentDescription = if (state.drawerState.isOpen) "close" else "open",
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
        SupportRTLModalNavigationDrawer(
            drawerState = state.drawerState,
            rtlLayout = true,
            drawerContent = {
                val successState = state as? ExamListState.Success

                //avoid compose
                var singleKeyword by remember { mutableStateOf(successState?.keyword ?: "") }

                LaunchedEffect(key1 = singleKeyword) {
                    model.filterPassType(
                        keyword = singleKeyword,
                        type = successState?.passFilter ?: PassFilter.ALL,
                        degree = successState?.degreeFilter ?: DegreeFilter.ALL,
                        currentYearIndex = successState?.currentYearIndex,
                        currentTermIndex = successState?.currentTermIndex
                    )
                }

                ExamListScreenDrawer(
                    keyword = singleKeyword,
                    passFilter = successState?.passFilter,
                    degreeFilter = successState?.degreeFilter,
                    currentYearIndex = successState?.currentYearIndex,
                    currentTermIndex = successState?.currentTermIndex,
                    selector = successState?.selector ?: emptyList(),
                    onKeywordChanged = { keyword ->
                        singleKeyword = keyword
                    },
                    onPassFilterChanged = { filter ->
                        model.filterPassType(
                            keyword = singleKeyword,
                            type = filter,
                            degree = successState?.degreeFilter ?: DegreeFilter.ALL,
                            currentYearIndex = successState?.currentYearIndex,
                            currentTermIndex = successState?.currentTermIndex
                        )
                    },
                    onDegreeFilterChanged = { filter ->
                        model.filterPassType(
                            keyword = singleKeyword,
                            type = successState?.passFilter ?: PassFilter.ALL,
                            degree = filter,
                            currentYearIndex = successState?.currentYearIndex,
                            currentTermIndex = successState?.currentTermIndex
                        )
                    },
                    onCurrentYearChanged = { yearIndex ->
                        model.filterPassType(
                            keyword = singleKeyword,
                            type = successState?.passFilter ?: PassFilter.ALL,
                            degree = successState?.degreeFilter ?: DegreeFilter.ALL,
                            currentYearIndex = yearIndex,
                            currentTermIndex = 0 // Reset term when year changes
                        )
                    },
                    onCurrentTermChanged = { termIndex ->
                        model.filterPassType(
                            keyword = singleKeyword,
                            type = successState?.passFilter ?: PassFilter.ALL,
                            degree = successState?.degreeFilter ?: DegreeFilter.ALL,
                            currentYearIndex = successState?.currentYearIndex,
                            currentTermIndex = termIndex
                        )
                    },
                    onResetFilters = {
                        singleKeyword = ""
                        model.filterPassType(
                            keyword = null,
                            type = PassFilter.ALL,
                            degree = DegreeFilter.ALL,
                            currentYearIndex = null,
                            currentTermIndex = null
                        )
                    }
                )
            },
            content = {
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
        )
    }

}

@Composable
fun ExamListScreenDrawer(
    keyword: String? = null,
    passFilter: PassFilter?,
    degreeFilter: DegreeFilter?,
    currentYearIndex: Int?,
    currentTermIndex: Int?,
    selector: List<Pair<YearSelectBean, List<TermSelectBean>>> = emptyList(),
    onKeywordChanged: (String) -> Unit = {},
    onPassFilterChanged: (PassFilter) -> Unit = {},
    onDegreeFilterChanged: (DegreeFilter) -> Unit = {},
    onCurrentYearChanged: (Int) -> Unit = {},
    onCurrentTermChanged: (Int) -> Unit = {},
    onResetFilters: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text(
            text = "搜索",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = keyword ?: "",
            onValueChange = onKeywordChanged,
            label = { Text("课程名称或教师姓名") },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(Modifier.height(8.dp))

        Text(
            text = "筛选",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        // Combined Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left column - Pass Status Filter
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "考试状态:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                PassFilterDropdown(
                    selectedFilter = passFilter ?: PassFilter.ALL,
                    enabled = passFilter != null,
                    onFilterChanged = onPassFilterChanged
                )
            }

            // Right column - Degree Filter
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "学位课程:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                DegreeFilterDropdown(
                    selectedFilter = degreeFilter ?: DegreeFilter.ALL,
                    enabled = degreeFilter != null,
                    onFilterChanged = onDegreeFilterChanged
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Year and Term Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left column - Year Filter
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "学年:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                YearFilterDropdown(
                    selector = selector,
                    currentYearIndex = currentYearIndex ?: 0,
                    enabled = currentYearIndex != null,
                    onYearChanged = onCurrentYearChanged
                )
            }

            // Right column - Term Filter
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "学期:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                TermFilterDropdown(
                    selector = selector,
                    currentYearIndex = currentYearIndex ?: 0,
                    currentTermIndex = currentTermIndex ?: 0,
                    enabled = currentTermIndex != null,
                    onTermChanged = onCurrentTermChanged
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "重置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重置筛选")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassFilterDropdown(
    selectedFilter: PassFilter,
    enabled: Boolean,
    onFilterChanged: (PassFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            readOnly = true,
            value = when (selectedFilter) {
                PassFilter.ALL -> "全部"
                PassFilter.PASS -> "通过"
                PassFilter.NOT_PASS -> "挂科"
                PassFilter.RE_PASS -> "补考"
            },
            onValueChange = {},
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            PassFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (filter) {
                                PassFilter.ALL -> "全部"
                                PassFilter.PASS -> "通过"
                                PassFilter.NOT_PASS -> "挂科"
                                PassFilter.RE_PASS -> "补考"
                            }
                        )
                    },
                    onClick = {
                        onFilterChanged(filter)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DegreeFilterDropdown(
    selectedFilter: DegreeFilter,
    enabled: Boolean,
    onFilterChanged: (DegreeFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            readOnly = true,
            value = when (selectedFilter) {
                DegreeFilter.ALL -> "全部"
                DegreeFilter.ONLY_DEGREE -> "学位课"
                DegreeFilter.NO_DEGREE -> "非学位课"
            },
            onValueChange = {},
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            DegreeFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (filter) {
                                DegreeFilter.ALL -> "全部"
                                DegreeFilter.ONLY_DEGREE -> "学位课"
                                DegreeFilter.NO_DEGREE -> "非学位课"
                            }
                        )
                    },
                    onClick = {
                        onFilterChanged(filter)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearFilterDropdown(
    selector: List<Pair<YearSelectBean, List<TermSelectBean>>>,
    currentYearIndex: Int,
    enabled: Boolean,
    onYearChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = if (selector.isNotEmpty() && currentYearIndex < selector.size) {
        selector[currentYearIndex].first
    } else null

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            readOnly = true,
            value = currentYear?.yearDisplay ?: "选择学年",
            onValueChange = {},
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            selector.forEachIndexed { index, (year, _) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = year.yearDisplay,
                            color = if (index == currentYearIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (index == currentYearIndex)
                                FontWeight.Bold
                            else
                                FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onYearChanged(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermFilterDropdown(
    selector: List<Pair<YearSelectBean, List<TermSelectBean>>>,
    currentYearIndex: Int,
    currentTermIndex: Int,
    enabled: Boolean,
    onTermChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val terms = if (selector.isNotEmpty() && currentYearIndex < selector.size) {
        selector[currentYearIndex].second
    } else emptyList()

    val currentTerm = if (terms.isNotEmpty() && currentTermIndex < terms.size) {
        terms[currentTermIndex]
    } else null

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            readOnly = true,
            value = currentTerm?.semesterDisplay ?: "选择学期",
            onValueChange = {},
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(8.dp)
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            terms.forEachIndexed { index, term ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = term.semesterDisplay,
                            color = if (index == currentTermIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (index == currentTermIndex)
                                FontWeight.Bold
                            else
                                FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onTermChanged(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
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
