package top.kagg886.eoa.pages.main.settings.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kborowy.colorpicker.KolorPicker
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.collapse.CollapsableTopAppBarScaffold
import top.kagg886.eoa.pages.main.about.AboutRoute
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.logcat.LogcatRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmRoute
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfile
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.shared.applyIf
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object SettingListRoute

@Composable
fun SettingListScreen() {
    val rootModel = rootViewModel()
    val nav = LocalNavController.current
    val mainRouteViewModel = mainViewModel()
    val mainState by mainRouteViewModel.collectAsState()
    val model = viewModel(key = mainState.toString()) {
        SettingsModel(mainState, mainRouteViewModel.database)
    }

    val state by model.collectAsState()

    model.collectSideEffect {

    }

    val rootState by rootModel.collectAsState()
    val color by rootState.color.collectAsState()
    val theme by rootState.theme.collectAsState()
    val module by rootState.module.collectAsState()
    val snack = LocalSnackBarHost.current
    SettingScreenContent(
        state,
        onLogoutButtonClicked = {
            nav.navigate(LogoutConfirmRoute)
        },
        onDetailButtonClicked = {
            nav.navigate(SettingsProfile)
        },

        color = color,
        theme = theme,
        module = module,
        onColorSettingsClicked = rootModel::postNewColorSetting,
        onThemeSettingsClicked = rootModel::postNewThemeSetting,
        onModuleChanged = rootModel::postEOAModuleSetting,
        onEggClicked = {
            snack.showSnackBar(SnackBarType.Error, "为什么要演奏春...")
        }
    )
}

@Composable
private fun SettingScreenContent(
    state: SettingsState,
    onDetailButtonClicked: () -> Unit,
    onLogoutButtonClicked: () -> Unit,

    color: Color,
    theme: AppSettingsMMKVType.AppTheme,
    module: List<EOAHomeModule>,
    onColorSettingsClicked: (Color) -> Unit,
    onThemeSettingsClicked: (AppSettingsMMKVType.AppTheme) -> Unit,
    onModuleChanged: (List<EOAHomeModule>) -> Unit = {},

    onEggClicked: () -> Unit,
) {
    CollapsableTopAppBarScaffold(
        modifier = Modifier.fillMaxSize(),
        background = {
            AnimatedContent(
                targetState = state,
                modifier = it.systemBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                when (it) {
                    is SettingsState.Failed -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "错误",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "同步错误",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = it.msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(
                                            alpha = 0.8f
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is SettingsState.Loading -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "加载中",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "正在同步用户信息...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is SettingsState.Success -> {
                        Column {
                            ProfileCard(
                                it.profile.avatar,
                                it.profile.name,
                                it.profile.studyName
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 详细信息按钮
                                OutlinedButton(
                                    onClick = onDetailButtonClicked,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "详细信息",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("详细信息")
                                }

                                // 登出按钮
                                Button(
                                    onClick = onLogoutButtonClicked,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "登出",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("登出")
                                }
                            }
                        }
                    }
                }
            }
        },
        title = {
            Text(
                text = "设置",
            )
        },
        navigationIcon = {
            BackIconButton()
        },
        content = { modifier ->
            val columnState = rememberLazyListState()
            LazyColumn(
                modifier.fixComposeListScrollToTopBug(columnState).fillMaxWidth(),
                state = columnState
            ) {
                item {
                    Text(
                        text = "外观设置",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                item {
                    var dialog by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text("系统主题") },
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
                }

                item {
                    var dialog by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text("主题色") },
                        trailingContent = {
                            Text(
                                BUILTIN_COLORS.entries.find { (_, value) -> value == color }?.key
                                    ?: "自定义",
                                color = color
                            )
                            if (dialog) {
                                DropdownMenu(
                                    modifier = Modifier.width(150.dp),
                                    expanded = true,
                                    onDismissRequest = { dialog = false }
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
                        modifier = Modifier.clickable { dialog = true },
                    )
                }

                item {
                    var dialog by remember { mutableStateOf(false) }
                    if (dialog) {
                        AlertDialog(
                            onDismissRequest = { dialog = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dialog = false
                                    }
                                ) {
                                    Text("确定")
                                }
                            },
                            title = { Text("自定义底部栏") },
                            text = {
                                LazyColumn {
                                    items(EOAHomeModule.entries) {
                                        ListItem(
                                            headlineContent = {
                                                Text(it.display)
                                            },
                                            leadingContent = {
                                                Checkbox(
                                                    checked = module.contains(it),
                                                    onCheckedChange = null,
                                                )
                                            },
                                            modifier = Modifier.applyIf(it !== EOAHomeModule.SUMMARY) {
                                                clickable {
                                                    val newModule = if (module.contains(it)) {
                                                        module - it
                                                    } else {
                                                        module + it
                                                    }
                                                    if (newModule.size > 4) {
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
                        supportingContent = { Text("自定义底部导航栏的内容。\n最多定制4条，多余的内容会存放进 '更多' 中") },
                        modifier = Modifier.clickable {
                            dialog = true
                        },
                    )
                }

                item {
                    Text(
                        text = "高级设置",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                item {
                    val nav = LocalNavController.current
                    ListItem(
                        headlineContent = {
                            Text("系统日志")
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.DeveloperMode,
                                contentDescription = "系统设置",
                            )
                        },
                        modifier = Modifier.clickable {
                            nav.navigate(LogcatRoute)
                        }
                    )
                }

                item {
                    val rootViewModel = rootViewModel()
                    ListItem(
                        headlineContent = {
                            Text("检查更新")
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Update,
                                contentDescription = "设置",
                            )
                        },
                        modifier = Modifier.clickable {
                            rootViewModel.checkUpdate()
                        }
                    )
                }

                item {
                    val nav = LocalNavController.current
                    ListItem(
                        headlineContent = {
                            Text("关于系统")
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "关于系统",
                            )
                        },
                        modifier = Modifier.clickable {
                            nav.navigate(AboutRoute)
                        }
                    )
                }
            }

        }
    )
}

@Composable
private fun ProfileCard(
    byteArray: ByteArray,
    name: String,
    grade: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            AsyncImage(
                model = byteArray,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = grade,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
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
