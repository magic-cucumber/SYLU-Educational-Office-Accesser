package top.kagg886.eoa.pages.main.home.exam.list.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.component.drawer.DrawerSheetPageScaffold
import top.kagg886.eoa.component.drawer.DrawerSheetPopupDirection
import top.kagg886.eoa.pages.main.home.exam.list.ExamListScreen
import top.kagg886.eoa.pages.main.home.exam.list.ExamListState
import top.kagg886.eoa.pages.main.home.exam.list.content.DegreeFilter
import top.kagg886.eoa.pages.main.home.exam.list.content.PassFilter
import top.kagg886.eoa.pages.main.home.exam.list.content.TermSelectBean
import top.kagg886.eoa.pages.main.home.exam.list.content.YearSelectBean
import top.kagg886.eoa.pages.main.home.exam.list.examListViewModelOrNull

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/6 15:27
 * ================================================
 */

@Serializable
data object ExamListFilterRoute

@Composable
fun ExamListFilterScreen() = ExamListScreen {
    val model = examListViewModelOrNull() ?: return@ExamListScreen
    val state by model.collectAsState()
    DrawerSheetPageScaffold(direction = DrawerSheetPopupDirection.RIGHT) {
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
    }
}


@Composable
private fun ExamListScreenDrawer(
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
