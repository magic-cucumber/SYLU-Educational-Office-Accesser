package top.kagg886.eoa.pages.main.settings.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalGlobalViewModelStoreOwner
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.collapse.CollapsableTopAppBarScaffold
import top.kagg886.eoa.pages.main.about.AboutRoute
import top.kagg886.eoa.pages.main.logcat.LogcatRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmRoute
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfile
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.pages.update.UpdateModel

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
    val ktorLogLevel by rootState.ktorLogLevel.collectAsState()

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
        ktorLogLevel = ktorLogLevel,
        onColorSettingsClicked = rootModel::postNewColorSetting,
        onThemeSettingsClicked = rootModel::postNewThemeSetting,
        onKtorLogLevelSettingsClicked = rootModel::postNewKtorLogLevelSetting
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreenContent(
    state: SettingsState,
    onDetailButtonClicked: () -> Unit,
    onLogoutButtonClicked: () -> Unit,

    color: Color,
    theme: AppSettingsMMKVType.AppTheme,
    ktorLogLevel: LogLevel,
    onColorSettingsClicked: (Color) -> Unit,
    onThemeSettingsClicked: (AppSettingsMMKVType.AppTheme) -> Unit,
    onKtorLogLevelSettingsClicked: (LogLevel) -> Unit
) {
    CollapsableTopAppBarScaffold(
        modifier = Modifier.fillMaxSize(),
        background = {
            AnimatedContent(
                targetState = state,
                modifier = it
            ) {
                when (it) {
                    is SettingsState.Failed -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            Row {
                                //详细信息按钮和退出登录按钮。
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
            }
        },
        title = {
            Text(
                text = "设置",
            )
        },
        navigationIcon =  {
            BackIconButton()
        },
        content = {
            val columnState = rememberLazyListState()
            LazyColumn(it.fixComposeListScrollToTopBug(columnState).fillMaxWidth(), state = columnState) {
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
                                    for (i in AppSettingsMMKVType.AppTheme.entities) {
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
                    var dialog by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = {
                            Text("网络日志级别")
                        },
                        trailingContent = {
                            Text(
                                when (ktorLogLevel) {
                                    LogLevel.ALL -> "ALL"
                                    LogLevel.HEADERS -> "HEAD"
                                    LogLevel.BODY -> "BODY"
                                    LogLevel.INFO -> "INFO"
                                    LogLevel.NONE -> "NONE"
                                }
                            )
                            if (dialog) {
                                DropdownMenu(
                                    expanded = true,
                                    onDismissRequest = {
                                        dialog = false
                                    }
                                ) {
                                    for (level in LogLevel.entries) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(level.name)
                                            },
                                            onClick = {
                                                onKtorLogLevelSettingsClicked(level)
                                                dialog = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            dialog = true
                        }
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
                    val updateState =
                        viewModel(viewModelStoreOwner = LocalGlobalViewModelStoreOwner.current) { UpdateModel() }
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
                            updateState.checkUpdate()
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
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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