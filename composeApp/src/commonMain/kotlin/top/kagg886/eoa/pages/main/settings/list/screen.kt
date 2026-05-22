package top.kagg886.eoa.pages.main.settings.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.about.AboutRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.advanced.AdvancedSettingsRoute
import top.kagg886.eoa.pages.main.settings.appearance.AppearanceSettingsRoute
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmRoute
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfile
import top.kagg886.eoa.pages.main.settings.sync.SyncSettingsRoute
import top.kagg886.eoa.pages.rootViewModel

@Serializable
data object SettingListRoute

@Composable
fun SettingListScreen() = MainScreen {
    val nav = LocalNavController.current
    val mainRouteViewModel = mainViewModelOrNull() ?: return@MainScreen

    val mainState by mainRouteViewModel.collectAsState()
    val model = viewModel(key = mainState.toString()) {
        SettingsModel(mainState, mainRouteViewModel.database)
    }

    val state by model.collectAsState()

    model.collectSideEffect {

    }

    SettingScreenContent(
        state,
        onLogoutButtonClicked = {
            nav.navigate(LogoutConfirmRoute)
        },
        onDetailButtonClicked = {
            nav.navigate(SettingsProfile)
        },
        onAppearanceSettingsClicked = {
            nav.navigate(AppearanceSettingsRoute)
        },
        onSyncSettingsClicked = {
            nav.navigate(SyncSettingsRoute)
        },
        onAdvancedSettingsClicked = {
            nav.navigate(AdvancedSettingsRoute)
        },
        onAboutClicked = {
            nav.navigate(AboutRoute)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreenContent(
    state: SettingsState,
    onDetailButtonClicked: () -> Unit,
    onLogoutButtonClicked: () -> Unit,
    onAppearanceSettingsClicked: () -> Unit,
    onSyncSettingsClicked: () -> Unit,
    onAdvancedSettingsClicked: () -> Unit,
    onAboutClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { BackIconButton() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedContent(
                targetState = state,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    .padding(top = 16.dp),
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
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
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

            Spacer(modifier = Modifier.height(16.dp))

            ListItem(
                headlineContent = { Text("外观") },
                leadingContent = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "外观设置",
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "进入",
                    )
                },
                modifier = Modifier.clickable {
                    onAppearanceSettingsClicked()
                }
            )

            ListItem(
                headlineContent = { Text("同步") },
                leadingContent = {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "同步设置",
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "进入",
                    )
                },
                modifier = Modifier.clickable {
                    onSyncSettingsClicked()
                }
            )

            ListItem(
                headlineContent = { Text("高级") },
                leadingContent = {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = "高级设置",
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "进入",
                    )
                },
                modifier = Modifier.clickable {
                    onAdvancedSettingsClicked()
                }
            )

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
                modifier = Modifier.clickable(onClick = onAboutClicked)
            )
        }
    }
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
