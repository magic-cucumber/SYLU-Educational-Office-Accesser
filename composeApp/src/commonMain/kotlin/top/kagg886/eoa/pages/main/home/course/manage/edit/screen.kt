@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.bottomsheet.BottomSheetPageScaffold
import top.kagg886.eoa.component.bottomsheet.SheetPosition
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.ai.AISettingsRoute
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
    val uri = LocalUriHandler.current
    val state by model.collectAsState()

    val stack = rememberToasterState()
    model.collectSideEffect {
        when (it) {
            is CourseEditSideEffect.Toast -> stack.showSnackBar(it.type, it.message)
            CourseEditSideEffect.NavigateBack -> Unit
        }
    }

    CourseEditScreenContent(
        model = model,
        state = state,
        snack = stack,
        onCourseModified = { model.modifyCourse(it) },
        onCourseInfoConfirmed = { model.confirmModifyCourse() },
        onAddRecord = { model.addRecord(it) },
        onUpdateRecord = { record, startTime, endTime ->
            model.updateRecord(record, startTime, endTime)
        },
        onDeleteRecord = { model.deleteRecord(it) },
        onLLMKeySelected = { model.selectLLMKey(it) },
        onGenerateButtonClicked = { input -> model.generateCourseByAI(input) },
        onImageCaptchaClicked = { model.generateCourseByImage() },
        onHelpClicked = { uri.openUri("${BuildConfig.MESSAGE_WEBSITE_URL}/course-overview.html#%E6%96%B0%E5%BB%BA%E8%AF%BE%E7%A8%8B") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditScreenContent(
    model: CourseEditModel,
    state: CourseEditState,
    snack: ToasterState,
    onCourseModified: (CourseEntity) -> Unit,
    onCourseInfoConfirmed: () -> Unit,
    onAddRecord: (LocalDate) -> Unit,
    onUpdateRecord: (CourseRecordEntity, LocalDateTime, LocalDateTime) -> Unit,
    onDeleteRecord: (CourseRecordEntity) -> Unit,
    onHelpClicked: () -> Unit,
    onLLMKeySelected: (LLMProviderEntity) -> Unit,
    onGenerateButtonClicked: (String) -> Unit,
    onImageCaptchaClicked: () -> Unit,
) {
    BottomSheetPageScaffold(
        snack = snack,
        maxExpandedHeight = LocalWindowInfo.current.containerDpSize.height * 0.9f,
        initialPopupType = SheetPosition.Expanded,
        popupTypeChangeRequest = { it != SheetPosition.PartiallyExpanded }
    ) {
        model.collectSideEffect {
            when (it) {
                is CourseEditSideEffect.NavigateBack -> close()
                else -> Unit
            }
        }
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                windowInsets = WindowInsets(),
                title = {
                    Text(
                        when (state) {
                            is CourseEditState.Loading -> "编辑课程"
                            is CourseEditState.Success -> "${if (state.courseId != null) "编辑" else "新建"}课程"
                        }
                    )
                },
                navigationIcon = {
                    BackIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        },
                        onBackPressed = {
                            close()
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onHelpClicked) {
                        Icon(Icons.AutoMirrored.Filled.Help,"help")
                    }
                    if (state is CourseEditState.Success) {
                        IconButton(
                            onClick = onCourseInfoConfirmed,
                            enabled = state.canSave
                        ) {
                            Icon(Icons.Default.Save, "save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            when (state) {
                is CourseEditState.Loading -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(16.dp))
                        Text("正在加载中，请稍等。")
                    }
                }

                is CourseEditState.Success -> {
                    val pagerState = rememberPagerState(0) { 3 }
                    val scope = rememberCoroutineScope()

                    Column(modifier = Modifier.weight(1f)) {
                        SecondaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
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
                                    invalidRecordIds = state.invalidRecordIds,
                                    onAddRecord = onAddRecord,
                                    onUpdateRecord = onUpdateRecord,
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditTime(
    startDate: LocalDate,
    allWeekNumber: Int,
    records: List<CourseRecordEntity>,
    invalidRecordIds: Set<Long>,
    onAddRecord: (LocalDate) -> Unit,
    onUpdateRecord: (CourseRecordEntity, LocalDateTime, LocalDateTime) -> Unit,
    onDeleteRecord: (CourseRecordEntity) -> Unit,
) {
    val pageState = rememberPagerState(0) { allWeekNumber }
    val scope = rememberCoroutineScope()
    var expandedDayOfWeek by remember { mutableStateOf<Int?>(null) }
    var pickerRequest by remember { mutableStateOf<TimePickerRequest?>(null) }

    LaunchedEffect(pageState.currentPage) {
        expandedDayOfWeek = null
    }

    Column(Modifier.fillMaxSize()) {
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

        if (invalidRecordIds.isNotEmpty()) {
            Text(
                text = "课程时间不能重叠，且结束时间必须晚于开始时间。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        HorizontalPager(
            state = pageState,
            modifier = Modifier.weight(1f),
        ) { weekNumberIndex ->
            val weekStartDate = startDate.plus(weekNumberIndex, DateTimeUnit.WEEK)

            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = (1..7).toList(),
                    key = { it }
                ) { dayOfWeek ->
                    val date = weekStartDate.plus(dayOfWeek - 1, DateTimeUnit.DAY)
                    val dayRecords = records
                        .filter { it.startTime.date == date }
                        .sortedBy { it.startTime }

                    CourseDayGroup(
                        dayOfWeek = dayOfWeek,
                        date = date,
                        records = dayRecords,
                        invalidRecordIds = invalidRecordIds,
                        expanded = expandedDayOfWeek == dayOfWeek,
                        onExpandedChange = {
                            expandedDayOfWeek = if (expandedDayOfWeek == dayOfWeek) {
                                null
                            } else {
                                dayOfWeek
                            }
                        },
                        onAddRecord = {
                            expandedDayOfWeek = dayOfWeek
                            onAddRecord(date)
                        },
                        onStartTimeClicked = {
                            pickerRequest = TimePickerRequest(it, TimeField.Start)
                        },
                        onEndTimeClicked = {
                            pickerRequest = TimePickerRequest(it, TimeField.End)
                        },
                        onDeleteRecord = onDeleteRecord,
                    )
                }
            }
        }
    }

    pickerRequest?.let { request ->
        val initialTime = when (request.field) {
            TimeField.Start -> request.record.startTime.time
            TimeField.End -> request.record.endTime.time
        }
        CourseTimePickerDialog(
            title = if (request.field == TimeField.Start) "选择开始时间" else "选择结束时间",
            initialTime = initialTime,
            onDismissRequest = { pickerRequest = null },
            onConfirm = { time ->
                val date = request.record.startTime.date
                when (request.field) {
                    TimeField.Start -> onUpdateRecord(
                        request.record,
                        date.atTime(time),
                        request.record.endTime,
                    )

                    TimeField.End -> onUpdateRecord(
                        request.record,
                        request.record.startTime,
                        date.atTime(time),
                    )
                }
                pickerRequest = null
            }
        )
    }
}

@Composable
private fun CourseDayGroup(
    dayOfWeek: Int,
    date: LocalDate,
    records: List<CourseRecordEntity>,
    invalidRecordIds: Set<Long>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onAddRecord: () -> Unit,
    onStartTimeClicked: (CourseRecordEntity) -> Unit,
    onEndTimeClicked: (CourseRecordEntity) -> Unit,
    onDeleteRecord: (CourseRecordEntity) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandedChange)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "周${getDayOfWeekText(dayOfWeek)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${date.month.number}月${date.day}日 · ${records.size} 条",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onAddRecord) {
                Icon(Icons.Default.Add, contentDescription = "新增上课时间")
            }

            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "course-time-expand-arrow"
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                expandFrom = Alignment.Top
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = shrinkVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                shrinkTowards = Alignment.Top
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize()
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 12.dp)
            ) {
                if (records.isEmpty()) {
                    Text(
                        text = "暂无上课时间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    for (record in records) {
                        key(record.id) {
                            val visibleState = remember {
                                MutableTransitionState(false).apply { targetState = true }
                            }

                            LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
                                if (visibleState.isIdle && !visibleState.currentState) {
                                    onDeleteRecord(record)
                                }
                            }

                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = fadeIn(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                CourseTimeRecordItem(
                                    record = record,
                                    isError = record.id?.let { it in invalidRecordIds } == true,
                                    onStartTimeClicked = { onStartTimeClicked(record) },
                                    onEndTimeClicked = { onEndTimeClicked(record) },
                                    onDeleteRecord = { visibleState.targetState = false  },
                                )
                            }
                        }

                    }
                }
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun CourseTimeRecordItem(
    modifier: Modifier = Modifier,
    record: CourseRecordEntity,
    isError: Boolean,
    onStartTimeClicked: () -> Unit,
    onEndTimeClicked: () -> Unit,
    onDeleteRecord: () -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadOnlyTimeField(
                label = "开始时间",
                value = record.startTime.time.toDisplayText(),
                isError = isError,
                onClick = onStartTimeClicked,
                modifier = Modifier.weight(1f),
            )
            ReadOnlyTimeField(
                label = "结束时间",
                value = record.endTime.time.toDisplayText(),
                isError = isError,
                onClick = onEndTimeClicked,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDeleteRecord) {
                Icon(Icons.Default.Delete, contentDescription = "删除上课时间")
            }
        }

        if (isError) {
            Text(
                text = "该时间段无效或与其他条目重叠",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ReadOnlyTimeField(
    label: String,
    value: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseTimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

private data class TimePickerRequest(
    val record: CourseRecordEntity,
    val field: TimeField,
)

private enum class TimeField {
    Start,
    End,
}

private fun LocalTime.toDisplayText(): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
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

        if (!providerSelectorEnabled) {
            val nav = LocalNavController.current
            val theme = MaterialTheme.colorScheme
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = theme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = theme.onErrorContainer)) {
                                append("当前未配置AI模型，")
                            }
                            withLink(
                                link = LinkAnnotation.Clickable(
                                    tag = "ai_settings",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = theme.error,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        pressedStyle = SpanStyle(
                                            color = theme.error.copy(alpha = 0.8f),
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ),
                                    linkInteractionListener = {
                                        nav.navigate(AISettingsRoute)
                                    }
                                )
                            ) {
                                append("点击进行配置")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

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
