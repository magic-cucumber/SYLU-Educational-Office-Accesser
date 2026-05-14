@file:OptIn(ExperimentalTime::class)

package top.kagg886.eoa.pages.main.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kborowy.colorpicker.KolorPicker
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.display
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.shared.applyIf
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
    val snack = LocalSnackBarHost.current

    AppearanceSettingsContent(
        color = color,
        theme = theme,
        module = module,
        systemWidgetRadius = systemWidgetRadius,
        showExperimentClass = showExperimentClass,
        onColorSettingsClicked = rootModel::postNewColorSetting,
        onThemeSettingsClicked = rootModel::postNewThemeSetting,
        onModuleChanged = rootModel::postEOAModuleSetting,
        onSystemWidgetRadiusChanged = rootModel::postSystemWidgetRadiusSetting,
        onShowExperimentClassChanged = rootModel::postShowExperimentClassSetting,
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
    onColorSettingsClicked: (Color) -> Unit,
    onThemeSettingsClicked: (AppSettingsMMKVType.AppTheme) -> Unit,
    onModuleChanged: (List<EOAHomeModule>) -> Unit = {},
    onSystemWidgetRadiusChanged: (Boolean) -> Unit,
    onShowExperimentClassChanged: (Boolean) -> Unit,
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
                        BUILTIN_COLORS.entries.find { (_, value) -> value == color }?.key
                            ?: "自定义",
                        color = color
                    )
                    if (colorDialog) {
                        DropdownMenu(
                            modifier = Modifier.width(150.dp),
                            expanded = true,
                            onDismissRequest = { colorDialog = false }
                        ) {
                            for ((key, builtInColor) in BUILTIN_COLORS) {
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
                        Column {
                            EOAHomeModule.entries.forEach { moduleItem ->
                                ListItem(
                                    headlineContent = {
                                        Text(moduleItem.display)
                                    },
                                    leadingContent = {
                                        Checkbox(
                                            checked = module.contains(moduleItem),
                                            onCheckedChange = null,
                                        )
                                    },
                                    modifier = Modifier.applyIf(moduleItem !== EOAHomeModule.SUMMARY) {
                                        clickable {
                                            val newModule = if (module.contains(moduleItem)) {
                                                module - moduleItem
                                            } else {
                                                module + moduleItem
                                            }
                                            if (newModule.size > 3) {
                                                return@clickable
                                            }
                                            onModuleChanged(newModule)
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = AlertDialogDefaults.containerColor
                                    )
                                )
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

private val BUILTIN_COLORS = mapOf(
    "姨妈红" to Color(188, 1, 4),
    "闪耀橙" to Color(255, 85, 34),
    "贝斯黄" to Color(255, 221, 136),
    "风祝绿" to Color(26, 240, 79),
    "拉格蓝" to Color(118, 145, 217)
)
