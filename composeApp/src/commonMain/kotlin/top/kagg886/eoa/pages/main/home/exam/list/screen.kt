package top.kagg886.eoa.pages.main.home.exam.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.kagg886.backend.database.dao.ExamEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.drawer.SupportRTLModalNavigationDrawer
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.home.exam.detail.ExamDetailRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus

@Serializable
data object ExamListRoute

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExamListScreen() = HomeScreen(
    route = NavigationRoute.EXAM,
    title = {
        Text("考试列表")
    },
    menu = {
        val mainViewModel = mainViewModel()
        val syncState by mainViewModel.collectAsState()
        val model = viewModel<ExamListViewModel>(key = syncState.toString()) {
            ExamListViewModel(mainViewModel.database, syncState)
        }
        val state by model.collectAsState()
        val scope = rememberCoroutineScope()
        IconButton(
            onClick = {
                scope.launch {
                    if (state.drawerState.isOpen) {
                        state.drawerState.close()
                    } else {
                        state.drawerState.open()
                    }
                }
            }
        ) {
            AnimatedContent(
                targetState = state.drawerState.currentValue,
                transitionSpec = {
                    if (initialState == DrawerValue.Open) {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    }
                }
            ) {
                when (it) {
                    DrawerValue.Closed -> Icon(
                        Icons.Default.FilterList,
                        contentDescription = "筛选"
                    )
                    DrawerValue.Open -> Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
        }
    }
) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<ExamListViewModel>(key = syncState.toString()) {
        ExamListViewModel(mainViewModel.database, syncState)
    }
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is ExamListSideEffect.NavigateToDetail -> {
                nav.navigate(ExamDetailRoute(it.examId))
            }
        }
    }
    val state by model.collectAsState()

    SupportRTLModalNavigationDrawer(
        drawerState = state.drawerState,
        rtlLayout = true,
        drawerContent = {
            ExamListScreenDrawer(
                state = state as? ExamListState.Success,
                onFilterChanged = { pass, degree, yearIndex, termIndex ->
                    model.filterPassType(pass, degree, yearIndex, termIndex)
                }
            )
        },
        content = {
            ExamListScreenContent(
                state,
                onExamItemClicked = {
                    model.navigateToDetail(it)
                },
            )
        }
    )
}

@Composable
fun ExamListScreenDrawer(
    state: ExamListState.Success?,
    onFilterChanged: (PassFilter, DegreeFilter, Int, Int) -> Unit = { _, _, _, _ -> },
) {
    val visible by remember(state) {
        derivedStateOf {
            state == null
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Filter Section
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Combined Filters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                            selectedFilter = state?.passFilter ?: PassFilter.ALL,
                            enabled = !visible,
                            onFilterChanged = { filter ->
                                if (!visible) {
                                    onFilterChanged(
                                        filter,
                                        state?.degreeFilter ?: DegreeFilter.ALL,
                                        state?.currentYearIndex ?: 0,
                                        state?.currentTermIndex ?: 0
                                    )
                                }
                            }
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
                            selectedFilter = state?.degreeFilter ?: DegreeFilter.ALL,
                            enabled = !visible,
                            onFilterChanged = { filter ->
                                if (!visible) {
                                    onFilterChanged(
                                        state?.passFilter ?: PassFilter.ALL,
                                        filter,
                                        state?.currentYearIndex ?: 0,
                                        state?.currentTermIndex ?: 0
                                    )
                                }
                            }
                        )
                    }
                }
                
                // Year and Term Filters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                            selector = state?.selector ?: emptyList(),
                            currentYearIndex = state?.currentYearIndex ?: 0,
                            enabled = !visible,
                            onYearChanged = { yearIndex ->
                                if (!visible) {
                                    onFilterChanged(
                                        state?.passFilter ?: PassFilter.ALL,
                                        state?.degreeFilter ?: DegreeFilter.ALL,
                                        yearIndex,
                                        0 // Reset term index when year changes
                                    )
                                }
                            }
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
                            selector = state?.selector ?: emptyList(),
                            currentYearIndex = state?.currentYearIndex ?: 0,
                            currentTermIndex = state?.currentTermIndex ?: 0,
                            enabled = !visible,
                            onTermChanged = { termIndex ->
                                if (!visible) {
                                    onFilterChanged(
                                        state?.passFilter ?: PassFilter.ALL,
                                        state?.degreeFilter ?: DegreeFilter.ALL,
                                        state?.currentYearIndex ?: 0,
                                        termIndex
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExamListScreenContent(
    state: ExamListState,
    onExamItemClicked: (ExamEntity) -> Unit = {},
) {
    when (state) {
        is ExamListState.Failed -> {
            ErrorPage(
                title = { Text("考试列表加载失败") },
                message = { Text("请检查系统日志") },
            )
        }

        is ExamListState.Loading -> {
            ExamListContent(
                null,
                onExamItemClicked
            )
        }

        is ExamListState.Success -> {
            ExamListContent(
                state,
                onExamItemClicked,
            )
        }
    }
}

@Composable
fun ExamListContent(
    state: ExamListState.Success?,
    onExamItemClicked: (ExamEntity) -> Unit,
) {
    if (state?.entity?.isEmpty() == true) {
        ErrorPage(
            title = { Text("没有考试")},
            message = { Text("点击菜单按钮以弹出筛选框")},
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    val layoutType = currentLayoutType()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
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
            .clickable(enabled = !showPlaceHolder) {
                exam?.let { onExamItemClicked(it) }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = exam?.name ?: "课程名称",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.placeholder(
                        visible = showPlaceHolder,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = "学分 × 绩点: ${exam?.let { "${it.credit} × ${it.gradePoint} = ${it.credit * it.gradePoint}" } ?: "0.0 × 0.0 = 0.0"}",
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