package top.kagg886.eoa.pages.main.settings.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.about.AboutRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.advanced.AdvancedSettingsRoute
import top.kagg886.eoa.pages.main.settings.ai.AISettingsRoute
import top.kagg886.eoa.pages.main.settings.appearance.AppearanceSettingsRoute
import top.kagg886.eoa.pages.main.settings.feedback.FeedbackRoute
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmRoute
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfile
import top.kagg886.eoa.pages.main.settings.sync.SyncSettingsRoute
import top.kagg886.eoa.pages.rootViewModel

@Serializable
data object SettingListRoute

@Composable
fun SettingListScreen() = MainScreen {
    val nav = LocalNavController.current
    val rootViewModel = rootViewModel()
    val mainRouteViewModel = mainViewModelOrNull() ?: return@MainScreen

    val mainState by mainRouteViewModel.collectAsState()
    val model = viewModel(key = mainState.toViewModelKey()) {
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
        onAISettingsClicked = {
            nav.navigate(AISettingsRoute)
        },
        onAdvancedSettingsClicked = {
            nav.navigate(AdvancedSettingsRoute)
        },
        onAboutClicked = {
            nav.navigate(AboutRoute)
        },
        onFeedbackClicked = {
            nav.navigate(FeedbackRoute)
        },
        onUpdateChecked = {
            rootViewModel.checkUpdate(false)
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
    onAISettingsClicked: () -> Unit,
    onAdvancedSettingsClicked: () -> Unit,
    onFeedbackClicked: () -> Unit,
    onUpdateChecked: () -> Unit,
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
            SettingScreenProfile(
                title = when (state) {
                    is SettingsState.Success -> state.profile.name
                    is SettingsState.Failed -> "同步错误"
                    SettingsState.Loading -> null
                },
                subTitle = when (state) {
                    is SettingsState.Success -> state.profile.studyName
                    is SettingsState.Failed -> state.msg
                    SettingsState.Loading -> null
                },
                containerColor = when (state) {
                    is SettingsState.Failed -> MaterialTheme.colorScheme.errorContainer
                    else -> CardDefaults.cardColors().containerColor
                },
                titleColor = when (state) {
                    is SettingsState.Failed -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                subTitleColor = when (state) {
                    is SettingsState.Failed -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                icon = {
                    AnimatedContent(targetState = state) { s ->
                        when (s) {
                            is SettingsState.Success -> AsyncImage(
                                model = s.profile.avatar,
                                contentDescription = "用户头像",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            is SettingsState.Failed -> Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "错误",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )

                            SettingsState.Loading -> Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .placeholder(
                                        visible = true,
                                        shape = CircleShape,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                            )
                        }
                    }
                },
                onDetailClicked = { if (state is SettingsState.Success) onDetailButtonClicked() },
                onLogoutButtonClicked = onLogoutButtonClicked,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

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
                modifier = Modifier.clickable(onClick = onAppearanceSettingsClicked)
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
                modifier = Modifier.clickable(onClick = onSyncSettingsClicked)
            )

            ListItem(
                headlineContent = { Text("AI管理") },
                leadingContent = {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "AI管理",
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "进入",
                    )
                },
                modifier = Modifier.clickable(onClick = onAISettingsClicked)
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
                modifier = Modifier.clickable(onClick = onAdvancedSettingsClicked)
            )
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
                modifier = Modifier.clickable(onClick = onUpdateChecked)
            )

            ListItem(
                headlineContent = {
                    Text("问题反馈")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "问题反馈",
                    )
                },
                modifier = Modifier.clickable(onClick = onFeedbackClicked)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreenProfile(
    title: String?,
    subTitle: String?,
    icon: @Composable () -> Unit,
    onDetailClicked: () -> Unit,
    onLogoutButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = CardDefaults.cardColors().containerColor,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subTitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val animatedContainerColor by animateColorAsState(containerColor)

    Card(
        onClick = onDetailClicked,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 / 状态图标
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title ?: "名称加载中",
                    style = MaterialTheme.typography.titleLarge,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.placeholder(
                        visible = title == null,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subTitle ?: "学年正在加载中",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.placeholder(
                        visible = subTitle == null,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            }

            // 登出
            IconButton(onClick = onLogoutButtonClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "登出",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
