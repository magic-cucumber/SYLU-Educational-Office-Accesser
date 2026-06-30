package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.showSnackBar

//新增为null，否则为id
@Serializable
data class CourseEditRoute(
    val id: Long? = null
)

@Composable
fun CourseEditScreen(route: CourseEditRoute) {
    val mainModel = mainViewModelOrNull() ?: return
    val mainState by mainModel.collectAsState()
    val llmRuntimes by mainModel.llmExecutors.collectAsState()

    val model = viewModel(
        key = (mainState.toViewModelKey().hashCode() * 31 + llmRuntimes.hashCode() + route.hashCode()).toString()
    ) {
        CourseEditModel(mainModel.database, route.id, llmRuntimes)
    }
    val nav = LocalNavController.current
    val state by model.collectAsState()

    val stack = rememberToasterState()
    model.collectSideEffect {
        when (it) {
            is CourseEditSideEffect.Toast -> stack.showSnackBar(it.type, it.message)
            is CourseEditSideEffect.NavigateBack -> nav.popBackStack()
        }
    }

    CourseEditScreenContent(
        state = state,
        snack = stack,
        onCourseModified = { model.modifyCourse(it) },
        onCourseInfoConfirmed = { model.confirmModifyCourse() },
        onCourseInfoDismissed = { nav.popBackStack() },
        onAddRecord = { weekNumber, dayOfWeek, periodOfDay ->
            model.addRecord(
                weekNumber,
                dayOfWeek,
                periodOfDay
            )
        },
        onDeleteRecord = { model.deleteRecord(it) },
        onLLMKeySelected = { model.selectLLMKey(it) },
        onGenerateButtonClicked = { input -> model.generateCourseByAI(input) },
        onImageCaptchaClicked = { model.generateCourseByImage() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditScreenContent(
    state: CourseEditState,
    snack: ToasterState,
    onCourseModified: (CourseEntity) -> Unit,
    onCourseInfoConfirmed: () -> Unit,
    onCourseInfoDismissed: () -> Unit,
    onAddRecord: (weekNumber: Int, dayOfWeek: Int, periodOfDay: Int) -> Unit,
    onDeleteRecord: (CourseRecordEntity) -> Unit,

    onLLMKeySelected: (LLMProviderEntity) -> Unit,
    onGenerateButtonClicked: (String) -> Unit,
    onImageCaptchaClicked: () -> Unit,
) {
    when (state) {
        is CourseEditState.Loading -> {
            DialogPageScaffold(
                title = { Text("编辑课程") },
                snack = snack,
                icon = { Icon(Icons.Default.Edit, "") },
                confirmButton = {}
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("正在加载中，请稍等。")
                }
            }
        }

        is CourseEditState.Success -> {
            val pagerState = rememberPagerState(0) { 3 }
            val scope = rememberCoroutineScope()

            DialogPageScaffold(
                title = { Text("${if (state.courseId !== null) "编辑" else "新建"}课程") },
                snack = snack,
                icon = { Icon(Icons.Default.Edit, "") },
                confirmButton = {
                    TextButton(
                        onClick = onCourseInfoConfirmed,
                        enabled = state.enableSaveButton
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onCourseInfoDismissed
                    ) {
                        Text("取消")
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = AlertDialogDefaults.containerColor,
                        tabs = {
                            Tab(
                                text = { Text("课程信息") },
                                selected = pagerState.currentPage == 0,
                                onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                            )
                            Tab(
                                text = { Text("时间编辑") },
                                selected = pagerState.currentPage == 1,
                                onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                            )
                            Tab(
                                text = { Text("AI生成") },
                                selected = pagerState.currentPage == 2,
                                onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                            )
                        }
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        when (it) {
                            0 -> CourseEditBasic(
                                course = state.courseInfo,
                                onCourseModified = onCourseModified
                            )

                            1 -> CourseEditTime(
                                startDate = state.startDate,
                                allWeekNumber = state.allWeekNumber,
                                records = state.recordInfo,
                                onAddRecord = onAddRecord,
                                onDeleteRecord = onDeleteRecord
                            )

                            2 -> CourseEditAI(
                                providers = state.llmKeys,
                                selectedProvider = state.selectLLMKey,
                                aiGenerating = state.aiGenerating,
                                onLLMKeySelected = onLLMKeySelected,
                                onGenerateButtonClicked = onGenerateButtonClicked,
                                onImageCaptchaClicked = onImageCaptchaClicked
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseEditBasic(
    course: CourseEntity,
    onCourseModified: (CourseEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var name by remember {
            mutableStateOf(course.name)
        }
        LaunchedEffect(name) {
            onCourseModified(course.copy(name = name))
        }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            label = { Text("课程名称") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        var teacherName by remember {
            mutableStateOf(course.teacherName)
        }

        LaunchedEffect(teacherName) {
            onCourseModified(course.copy(teacherName = teacherName))
        }

        OutlinedTextField(
            value = teacherName,
            onValueChange = {
                teacherName = it
            },
            label = { Text("教师姓名") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )


        var classroomName by remember {
            mutableStateOf(course.classroomName)
        }
        LaunchedEffect(classroomName) {
            onCourseModified(course.copy(classroomName = classroomName))
        }
        OutlinedTextField(
            value = classroomName,
            onValueChange = {
                classroomName = it
            },
            label = { Text("教室名称") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        var credits by remember {
            mutableStateOf(course.credits.toString())
        }

        LaunchedEffect(credits) {
            val parsedCredits = credits.toFloatOrNull() ?: course.credits
            onCourseModified(course.copy(credits = parsedCredits))
        }

        OutlinedTextField(
            value = credits,
            onValueChange = {
                credits = it
            },
            label = { Text("学分") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked = course.isDegreeRequired,
                onCheckedChange = {
                    onCourseModified(course.copy(isDegreeRequired = it))
                }
            )
            Text("学位课")
        }
    }
}

@Composable
private fun CourseEditTime(
    startDate: LocalDate,
    allWeekNumber: Int,
    records: List<CourseRecordEntity>,
    onAddRecord: (weekNumber: Int, dayOfWeek: Int, periodOfDay: Int) -> Unit,
    onDeleteRecord: (CourseRecordEntity) -> Unit,
) {
    val pageState = rememberPagerState(0) { allWeekNumber }
    val scope = rememberCoroutineScope()

    Column {
        // Week selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (pageState.currentPage > 0) {
                        scope.launch { pageState.animateScrollToPage(pageState.currentPage - 1) }
                    }
                },
                enabled = pageState.currentPage > 0
            ) {
                Text("上一周")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "第 ${pageState.currentPage + 1} 周",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = {
                    if (pageState.currentPage < allWeekNumber - 1) {
                        scope.launch { pageState.animateScrollToPage(pageState.currentPage + 1) }
                    }
                },
                enabled = pageState.currentPage < allWeekNumber - 1
            ) {
                Text("下一周")
            }
        }

        // Course timetable
        HorizontalPager(
            state = pageState,
        ) { weekNumberIndex ->
            val weekNumber = weekNumberIndex + 1
            val weekStartDate = startDate.plus(weekNumberIndex, DateTimeUnit.WEEK)

            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // Timetable grid
                Box(Modifier.fillMaxWidth()) {
                    // Table header row with day of week
                    Row(Modifier.fillMaxWidth()) {
                        // Empty cell for top-left corner
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(40.dp)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("周/节", style = MaterialTheme.typography.bodySmall)
                        }

                        // Day headers
                        for (dayOfWeek in 1..7) {
                            val currentDate = weekStartDate.plus(dayOfWeek - 1, DateTimeUnit.DAY)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .padding(1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "周${getDayOfWeekText(dayOfWeek)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${currentDate.month.number}/${currentDate.day}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Timetable rows
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                        for (periodOfDay in 1..12) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Period number
                                Box(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .height(40.dp)
                                        .padding(1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$periodOfDay",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Checkboxes for each day
                                for (dayOfWeek in 1..7) {
                                    // Check if this slot has a record
                                    val hasRecord = records.any {
                                        it.weekNumber == weekNumber &&
                                                it.dayOfWeek == dayOfWeek &&
                                                it.periodOfDay == periodOfDay
                                    }

                                    val record = records.find {
                                        it.weekNumber == weekNumber &&
                                                it.dayOfWeek == dayOfWeek &&
                                                it.periodOfDay == periodOfDay
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .padding(1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Checkbox(
                                            checked = hasRecord,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    // Add record
                                                    onAddRecord(weekNumber, dayOfWeek, periodOfDay)
                                                } else if (record != null) {
                                                    // Delete record
                                                    onDeleteRecord(record)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CourseEditAI(
    providers: List<LLMProviderEntity>,
    selectedProvider: LLMProviderEntity?,
    aiGenerating: String?,
    onLLMKeySelected: (LLMProviderEntity) -> Unit,
    onGenerateButtonClicked: (String) -> Unit,
    onImageCaptchaClicked: () -> Unit
) {
    var inputMessage by remember {
        mutableStateOf("")
    }

    var providerMenuExpanded by remember { mutableStateOf(false) }
    val providerSelectorEnabled = providers.isNotEmpty()
    val aiInputEnabled = aiGenerating == null && providerSelectorEnabled

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(
            expanded = providerMenuExpanded,
            onExpandedChange = {
                if (providerSelectorEnabled) {
                    providerMenuExpanded = !providerMenuExpanded
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedProvider?.displayName ?: "未配置AI模型",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = providerMenuExpanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                label = { Text("AI模型") },
                enabled = providerSelectorEnabled
            )

            ExposedDropdownMenu(
                expanded = providerMenuExpanded,
                onDismissRequest = { providerMenuExpanded = false }
            ) {
                providers.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            ListItem(
                                headlineContent = { Text(provider.displayName) },
                                supportingContent = {
                                    if (provider.modelDescription.isNotBlank()) {
                                        Text(
                                            provider.modelDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MenuDefaults.containerColor
                                )
                            )
                        },
                        onClick = {
                            onLLMKeySelected(provider)
                            providerMenuExpanded = false
                        },
                        enabled = aiInputEnabled
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = inputMessage,
            onValueChange = { inputMessage = it },
            label = { Text("输入课程的自然信息") },
            minLines = 3,
            enabled = aiInputEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onGenerateButtonClicked(inputMessage) },
                modifier = Modifier.weight(1f),
                enabled = aiGenerating == null && selectedProvider != null
            ) {
                Text(text = aiGenerating ?: "生成")
            }

            Spacer(Modifier.width(16.dp))
            Text("或")
            Spacer(Modifier.width(16.dp))

            OutlinedButton(
                onClick = onImageCaptchaClicked,
                modifier = Modifier.weight(1f),
                enabled = aiGenerating == null && selectedProvider != null && selectedProvider.supportMultimodal
            ) {
                Text(text = aiGenerating ?: "选择图片")
            }
        }
    }

}

private val LLMProviderEntity.displayName: String
    get() = modelRemark.ifBlank { modelName }

// Helper function to convert numeric day of week to Chinese text
private fun getDayOfWeekText(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "日"
        else -> "$dayOfWeek"
    }
}
