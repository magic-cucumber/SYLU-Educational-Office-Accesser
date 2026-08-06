@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
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
        onImageCaptchaClicked = { model.generateCourseByImage() },
        onHelpClicked = { uri.openUri("https://eoa.kagg886.top/course-overview.html#%E6%96%B0%E5%BB%BA%E8%AF%BE%E7%A8%8B") }
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
    onAddRecord: (weekNumber: Int, dayOfWeek: Int, periodOfDay: Int) -> Unit,
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
                            enabled = state.enableSaveButton
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

            // 扫选模式状态：长按进入，扫过自动切换，松手退出
            var sweepMode by remember { mutableStateOf(false) }
            val sweptCells = remember { mutableSetOf<Pair<Int, Int>>() }
            val cellBounds = remember { mutableMapOf<Pair<Int, Int>, Rect>() }
            val rowLabelBounds = remember { mutableMapOf<Int, Rect>() }
            var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            val latestRecords by rememberUpdatedState(records)

            // iOS 桌面编辑风格的抖动动画
            val jiggleTransition = rememberInfiniteTransition(label = "sweepJiggle")
            val jiggleAngle = jiggleTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 110,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "jiggleAngle"
            )
            // 命中测试并切换格子状态，一次扫选中每个格子只切换一次
            fun toggleCellAt(position: Offset): Pair<Int, Int>? {
                val rootPosition = gridCoordinates?.localToRoot(position) ?: return null
                val cell = cellBounds.entries.firstOrNull { it.value.contains(rootPosition) }?.key
                    ?: return null
                if (!sweptCells.add(cell)) return cell
                val (dayOfWeek, periodOfDay) = cell
                val record = latestRecords.find {
                    it.weekNumber == weekNumber &&
                            it.dayOfWeek == dayOfWeek &&
                            it.periodOfDay == periodOfDay
                }
                if (record != null) {
                    onDeleteRecord(record)
                } else {
                    onAddRecord(weekNumber, dayOfWeek, periodOfDay)
                }
                return cell
            }

            // 长按周/节列时，反选当前行的全部格子
            fun toggleRowAt(position: Offset): Boolean {
                val rootPosition = gridCoordinates?.localToRoot(position) ?: return false
                val periodOfDay = rowLabelBounds.entries
                    .firstOrNull { it.value.contains(rootPosition) }
                    ?.key
                    ?: return false
                val rowRecords = latestRecords.filter {
                    it.weekNumber == weekNumber && it.periodOfDay == periodOfDay
                }
                for (dayOfWeek in 1..7) {
                    val record = rowRecords.find { it.dayOfWeek == dayOfWeek }
                    if (record != null) {
                        onDeleteRecord(record)
                    } else {
                        onAddRecord(weekNumber, dayOfWeek, periodOfDay)
                    }
                }
                return true
            }

            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // Timetable grid
                Box(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { gridCoordinates = it }
                        .pointerInput(weekNumber) {
                            awaitEachGesture {
                                val firstDown = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )

                                val slop = viewConfiguration.touchSlop

                                /*
                                 * 超时前：
                                 * - 抬起：取消
                                 * - 移动超过 touchSlop：取消
                                 * - 始终保持按下直到超时：判定为长按
                                 */
                                val longPressed = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull {
                                            it.id == firstDown.id
                                        } ?: return@withTimeoutOrNull false

                                        if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                            return@withTimeoutOrNull false
                                        }

                                        val offset = change.position - firstDown.position
                                        if (offset.getDistanceSquared() > slop * slop) {
                                            return@withTimeoutOrNull false
                                        }
                                    }

                                    false
                                } ?: true // 发生超时，说明长按成功

                                if (!longPressed) {
                                    return@awaitEachGesture
                                }

                                if (toggleRowAt(firstDown.position)) {
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull {
                                                it.id == firstDown.id
                                            } ?: break

                                            if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                                change.consume()
                                                break
                                            }

                                            change.consume()
                                        }
                                    } finally {
                                        sweepMode = false
                                        sweptCells.clear()
                                    }
                                    return@awaitEachGesture
                                }

                                sweepMode = true
                                sweptCells.clear()

                                toggleCellAt(firstDown.position)

                                var pointerId = firstDown.id
                                try {
                                    sweepLoop@ while (true) {
                                        // 当前按压期间进行扫选，直到抬起
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull {
                                                it.id == pointerId
                                            } ?: continue

                                            if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                                // 阻止 Checkbox 在抬起时触发点击
                                                change.consume()
                                                break
                                            }

                                            if (change.position != change.previousPosition) {
                                                toggleCellAt(change.position)
                                            }

                                            change.consume()
                                        }

                                        /*
                                         * 抬起后继续保持扫选模式。
                                         * 500ms 内再次按下则继续，否则结束。
                                         */
                                        val nextDown = withTimeoutOrNull(500L) {
                                            awaitFirstDown(
                                                requireUnconsumed = false,
                                                pass = PointerEventPass.Initial
                                            )
                                        } ?: break@sweepLoop

                                        pointerId = nextDown.id

                                        // 后续按下属于扫选模式，阻止 Checkbox 接收该按下事件
                                        nextDown.consume()

                                        // 再次按下时立即命中当前位置
                                        toggleCellAt(nextDown.position)
                                    }
                                } finally {
                                    sweepMode = false
                                    sweptCells.clear()
                                }
                            }
                        }
                ) {
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
                                        .padding(1.dp)
                                        .onGloballyPositioned {
                                            rowLabelBounds[periodOfDay] = it.boundsInRoot()
                                        },
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
                                            .padding(1.dp)
                                            .onGloballyPositioned {
                                                cellBounds[dayOfWeek to periodOfDay] =
                                                    it.boundsInRoot()
                                            }
                                            .graphicsLayer {
                                                val direction =
                                                    if ((dayOfWeek + periodOfDay) % 2 == 0) 1f else -1f

                                                rotationZ = if (sweepMode) {
                                                    jiggleAngle.value * direction
                                                } else {
                                                    0f
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Checkbox(
                                            checked = hasRecord,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    onAddRecord(
                                                        weekNumber,
                                                        dayOfWeek,
                                                        periodOfDay
                                                    )
                                                } else if (record != null) {
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
