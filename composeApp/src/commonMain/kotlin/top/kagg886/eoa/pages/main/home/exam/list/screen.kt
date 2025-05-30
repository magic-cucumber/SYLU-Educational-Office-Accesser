package top.kagg886.eoa.pages.main.home.exam.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                    state.drawerState.open()
                }
            }
        ) {
            Icon(Icons.Default.Menu, null)
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
                onFilterChanged = { pass, degree ->
                    model.filterPassType(pass, degree)
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
    onFilterChanged: (PassFilter, DegreeFilter) -> Unit = { _, _ -> },
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
                                        state?.degreeFilter ?: DegreeFilter.ALL
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
                                        filter
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
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExamItem(
                    exam = exam[0],
                    modifier = Modifier.weight(0.5f),
                    onExamItemClicked = onExamItemClicked
                )

                if (exam.size == 1) {
                    return@Row
                }
                Spacer(Modifier.width(16.dp))

                ExamItem(
                    exam = exam[1],
                    modifier = Modifier.weight(0.5f),
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