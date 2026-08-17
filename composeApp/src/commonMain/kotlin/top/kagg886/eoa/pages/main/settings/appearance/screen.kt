@file:OptIn(ExperimentalTime::class)

package top.kagg886.eoa.pages.main.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kborowy.colorpicker.KolorPicker
import com.kborowy.colorpicker.config.PickerConfig
import com.kborowy.colorpicker.config.TrackConfig
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.display
import top.kagg886.eoa.pages.main.home.icon
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.Platform
import top.kagg886.util.current
import kotlin.time.ExperimentalTime

@Serializable
data object AppearanceSettingsRoute

@Composable
fun AppearanceSettingsScreen() = MainScreen {
    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val color by rootState.color.collectAsState()
    val theme by rootState.theme.collectAsState()
    val module by rootState.module.collectAsState()
    val systemWidgetRadius by rootState.systemWidgetRadius.collectAsState()
    val showExperimentClass by rootState.showExperimentClass.collectAsState()
    val hideWeekendCourse by rootState.hideWeekendCourse.collectAsState()
    val snack = LocalSnackBarHost.current

    AppearanceSettingsContent(
        color = color,
        theme = theme,
        module = module,
        systemWidgetRadius = systemWidgetRadius,
        showExperimentClass = showExperimentClass,
        hideWeekendCourse = hideWeekendCourse,
        onColorSettingsClicked = rootModel::postNewColorSetting,
        onThemeSettingsClicked = rootModel::postNewThemeSetting,
        onModuleChanged = rootModel::postEOAModuleSetting,
        onSystemWidgetRadiusChanged = rootModel::postSystemWidgetRadiusSetting,
        onShowExperimentClassChanged = rootModel::postShowExperimentClassSetting,
        onHideWeekendCourseChanged = rootModel::postHideWeekendCourseSetting,
        onEggClicked = {
            snack.showSnackBar(SnackBarType.Error, "为什么要演奏春...")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsContent(
    color: Color,
    theme: AppSettingsMMKVType.AppTheme,
    module: List<EOAHomeModule>,
    systemWidgetRadius: Boolean,
    showExperimentClass: Boolean,
    hideWeekendCourse: Boolean,
    onColorSettingsClicked: (Color) -> Unit,
    onThemeSettingsClicked: (AppSettingsMMKVType.AppTheme) -> Unit,
    onModuleChanged: (List<EOAHomeModule>) -> Unit = {},
    onSystemWidgetRadiusChanged: (Boolean) -> Unit,
    onShowExperimentClassChanged: (Boolean) -> Unit,
    onHideWeekendCourseChanged: (Boolean) -> Unit,
    onEggClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
                navigationIcon = { BackIconButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            var dialog by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("系统主题") },
                leadingContent = {
                    Icon(
                        Icons.Default.Contrast,
                        "主题色"
                    )
                },
                trailingContent = {
                    Text(
                        when (theme) {
                            AppSettingsMMKVType.AppTheme.Light -> "浅色"
                            AppSettingsMMKVType.AppTheme.Dark -> "深色"
                            AppSettingsMMKVType.AppTheme.SystemDefault -> "跟随系统"
                        }
                    )
                    if (dialog) {
                        DropdownMenu(
                            modifier = Modifier.width(150.dp),
                            expanded = true,
                            onDismissRequest = { dialog = false }
                        ) {
                            for (i in AppSettingsMMKVType.AppTheme.entries) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (i) {
                                                AppSettingsMMKVType.AppTheme.Light -> "浅色"
                                                AppSettingsMMKVType.AppTheme.Dark -> "深色"
                                                AppSettingsMMKVType.AppTheme.SystemDefault -> "跟随系统"
                                            }
                                        )
                                    },
                                    onClick = {
                                        onThemeSettingsClicked(i)
                                        dialog = false
                                    }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.clickable { dialog = true },
            )

            var colorDialog by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("主题色") },
                leadingContent = {
                    Icon(
                        Icons.Default.Colorize,
                        "主题色"
                    )
                },
                trailingContent = {
                    Text(
                        AppSettingsMMKV.presetsColor.entries.find { (_, value) -> value == color }?.key
                            ?: "自定义",
                        color = color
                    )
                    if (colorDialog) {
                        DropdownMenu(
                            modifier = Modifier.width(150.dp),
                            expanded = true,
                            onDismissRequest = { colorDialog = false }
                        ) {
                            for ((key, builtInColor) in AppSettingsMMKV.presetsColor) {
                                var count by remember {
                                    mutableStateOf(0)
                                }
                                DropdownMenuItem(
                                    text = { Text(key) },
                                    leadingIcon = {
                                        Box(
                                            Modifier.size(16.dp).background(color = builtInColor)
                                        )
                                    },
                                    onClick = {
                                        onColorSettingsClicked(builtInColor)
                                        if (key == "贝斯黄") {
                                            count++
                                        }
                                        if (count == 5) {
                                            onEggClicked()
                                        }
                                    }
                                )
                            }

                            var pickerDialog by remember { mutableStateOf(false) }

                            if (pickerDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        pickerDialog = false
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                pickerDialog = false
                                            }
                                        ) {
                                            Text("确定")
                                        }
                                    },
                                    title = { Text("自定义取色") },
                                    text = {
                                        KolorPicker(
                                            initialColor = color,
                                            onColorSelected = onColorSettingsClicked,
                                            pickerConfig = PickerConfig.Default,
                                            alphaTrackConfig = TrackConfig.Default,
                                            hueTrackConfig = TrackConfig.Default,
                                            modifier = Modifier.width(250.dp).height(200.dp),
                                        )
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("自定义") },
                                onClick = { pickerDialog = true }
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { colorDialog = true },
            )

            var moduleDialog by remember { mutableStateOf(false) }
            if (moduleDialog) {
                val reorderState = rememberReorderState<EOAHomeModule>()
                val dialogModules = module + EOAHomeModule.entries.filter { it !in module }

                AlertDialog(
                    onDismissRequest = { moduleDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                moduleDialog = false
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    title = { Text("自定义底部栏") },
                    text = {
                        ReorderContainer(
                            state = reorderState,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(dialogModules, key = { it.name }) { moduleItem ->
                                    val selected = moduleItem in module
                                    val switchEnabled = moduleItem != EOAHomeModule.SUMMARY && (selected || module.size < 3)
                                    ReorderableItem(
                                        state = reorderState,
                                        key = moduleItem.name,
                                        data = moduleItem,
                                        enabled = selected && moduleItem != EOAHomeModule.SUMMARY,
                                        onDrop = { dragState ->
                                            val draggedModule = dragState.data
                                            val fromIndex = module.indexOf(draggedModule)
                                            val toIndex = module.indexOf(moduleItem)
                                            if (
                                                draggedModule != EOAHomeModule.SUMMARY &&
                                                moduleItem != EOAHomeModule.SUMMARY &&
                                                moduleItem in module
                                            ) {
                                                onModuleChanged(
                                                    module.toMutableList().apply {
                                                        removeAt(fromIndex)
                                                        add(toIndex, draggedModule)
                                                    }
                                                )
                                            }
                                        },
                                        onDragEnter = { dragState ->
                                            val draggedModule = dragState.data
                                            val fromIndex = module.indexOf(draggedModule)
                                            val toIndex = module.indexOf(moduleItem)
                                            if (
                                                draggedModule != EOAHomeModule.SUMMARY &&
                                                moduleItem != EOAHomeModule.SUMMARY &&
                                                moduleItem in module &&
                                                fromIndex != toIndex
                                            ) {
                                                onModuleChanged(
                                                    module.toMutableList().apply {
                                                        removeAt(fromIndex)
                                                        add(toIndex, draggedModule)
                                                    }
                                                )
                                            }
                                        },
                                        draggableContent = {
                                            ModuleListItem(
                                                moduleItem = moduleItem,
                                                selected = selected,
                                                switchEnabled = switchEnabled,
                                                onSelectedChanged = {},
                                                isDragShadow = true,
                                            )
                                        }
                                    ) {
                                        ModuleListItem(
                                            moduleItem = moduleItem,
                                            selected = selected,
                                            switchEnabled = switchEnabled,
                                            onSelectedChanged = { checked ->
                                                when {
                                                    moduleItem == EOAHomeModule.SUMMARY -> Unit
                                                    checked && module.size < 3 -> {
                                                        onModuleChanged(module + moduleItem)
                                                    }
                                                    !checked -> {
                                                        onModuleChanged(module - moduleItem)
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .animateItem()
                                                .graphicsLayer {
                                                    alpha = if (isDragging) 0f else 1f
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            ListItem(
                headlineContent = { Text("底部栏定制") },
                supportingContent = { Text("自定义底部导航栏的内容。\n最多定制3条，多余的内容会存放进 '更多' 中") },
                modifier = Modifier.clickable {
                    moduleDialog = true
                },
                leadingContent = {
                    Icon(
                        Icons.Default.CallToAction,
                        "主题色"
                    )
                },
            )

            if (Platform.current is Platform.Android) {
                ListItem(
                    headlineContent = { Text("小组件圆角跟随系统") },
                    supportingContent = { Text("部分OS上可能无法获取正确的圆角，此时请关闭此设置。\n小组件刷新时间不固定，手动刷新后至少冷却半小时以上才会继续响应刷新，部分系统刷新时间可能会超过1天") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Widgets,
                            "小组件圆角"
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = systemWidgetRadius,
                            onCheckedChange = onSystemWidgetRadiusChanged
                        )
                    }
                )
            }

            ListItem(
                headlineContent = { Text("隐藏周末课程") },
                supportingContent = { Text("若周六/日均无课程，则课表页周六/日会被隐藏。") },
                leadingContent = {
                    Icon(
                        Icons.Default.EventBusy,
                        "隐藏周末课程"
                    )
                },
                trailingContent = {
                    Switch(
                        checked = hideWeekendCourse,
                        onCheckedChange = onHideWeekendCourseChanged
                    )
                }
            )

            ListItem(
                headlineContent = { Text("显示实验课提示") },
                supportingContent = { Text("在概要页面显示本周的实验课列表，暂不支持查看本学期所有的实验课") },
                leadingContent = {
                    Icon(
                        Icons.Default.Science,
                        "实验课"
                    )
                },
                trailingContent = {
                    Switch(
                        checked = showExperimentClass,
                        onCheckedChange = onShowExperimentClassChanged
                    )
                }
            )
        }
    }
}
@Composable
private fun ModuleListItem(
    moduleItem: EOAHomeModule,
    selected: Boolean,
    switchEnabled: Boolean,
    onSelectedChanged: (Boolean) -> Unit,
    isDragShadow: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(moduleItem.display)
        },
        leadingContent = {
            Icon(
                imageVector = moduleItem.icon,
                contentDescription = moduleItem.display
            )
        },
        trailingContent = {
            Switch(
                checked = selected,
                enabled = switchEnabled,
                onCheckedChange = onSelectedChanged,
            )
        },
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = if (isDragShadow) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                AlertDialogDefaults.containerColor
            }
        )
    )
}
