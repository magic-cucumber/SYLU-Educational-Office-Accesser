package top.kagg886.eoa.pages.main.settings.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lexilabs.basic.sound.AudioState
import app.lexilabs.basic.sound.ExperimentalBasicSound
import coil3.compose.AsyncImage
import kotlinx.coroutines.isActive
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalMediaPlayer
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.logcat.LogcatRoute
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.about.AboutRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.settings.appearance.AppearanceSettingsRoute
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmRoute
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfile
import top.kagg886.eoa.pages.main.settings.sync.SyncSettingsRoute
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import kotlin.math.*
import kotlin.time.Clock

@Serializable
data object SettingListRoute

@Composable
fun SettingListScreen() = MainScreen {
    val nav = LocalNavController.current
    val mainRouteViewModel = mainViewModel()
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
        onAboutClicked = {
            nav.navigate(AboutRoute)
        },
        onLogcatClicked = {
            nav.navigate(LogcatRoute)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalBasicSound::class)
@Composable
private fun SettingScreenContent(
    state: SettingsState,
    onDetailButtonClicked: () -> Unit,
    onLogoutButtonClicked: () -> Unit,
    onAppearanceSettingsClicked: () -> Unit,
    onSyncSettingsClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    onLogcatClicked: () -> Unit,
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                    val maxSwingAngle = 10f
                                    val spinDurationMillis = 2400f

                                    val player = LocalMediaPlayer.current
                                    val state by player.audioState.collectAsState()
                                    val isApril = remember {
                                        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                                        date.month == Month.APRIL && date.day == 1
                                    }

                                    val rotation = run {
                                        if (!isApril) {
                                            return@run Animatable(0f)
                                        }
                                        val rotation = remember { Animatable(-maxSwingAngle) }
                                        var angularVelocity by remember { mutableFloatStateOf(0f) }

                                        LaunchedEffect(state) {
                                            var lastFrameNanos = withFrameNanos { it }

                                            if (state == AudioState.PLAYING) {
                                                val targetVelocity = 360f / spinDurationMillis
                                                val accelerationWindowMs = 220f

                                                while (isActive) {
                                                    val frameNanos = withFrameNanos { it }
                                                    val deltaMs =
                                                        (frameNanos - lastFrameNanos) / 1_000_000f
                                                    lastFrameNanos = frameNanos

                                                    val blend = min(1f, deltaMs / accelerationWindowMs)
                                                    angularVelocity +=
                                                        (targetVelocity - angularVelocity) * blend
                                                    rotation.snapTo(rotation.value + angularVelocity * deltaMs)
                                                }
                                            } else {
                                                val amplitude = maxSwingAngle
                                                val periodMs = 1600f
                                                val omega = (2f * PI.toFloat()) / periodMs
                                                val initialAngle =
                                                    (rotation.value / amplitude).coerceIn(-1f, 1f)
                                                var phase = asin(initialAngle)

                                                if (angularVelocity < 0f) {
                                                    phase = PI.toFloat() - phase
                                                }

                                                var elapsedMs = 0f
                                                while (isActive) {
                                                    val frameNanos = withFrameNanos { it }
                                                    val deltaMs =
                                                        (frameNanos - lastFrameNanos) / 1_000_000f
                                                    lastFrameNanos = frameNanos
                                                    elapsedMs += deltaMs

                                                    val angle = amplitude * sin(omega * elapsedMs + phase)
                                                    angularVelocity =
                                                        amplitude * omega * cos(omega * elapsedMs + phase)
                                                    rotation.snapTo(angle)
                                                }
                                            }
                                        }

                                        return@run rotation
                                    }

                                    val main = mainViewModel()
                                    LaunchedEffect(state) {
                                        when (state) {
                                            is AudioState.ERROR -> main.toast(SnackBarType.Error, (state as AudioState.ERROR).message)
                                            else -> Unit
                                        }
                                    }

                                    AsyncImage(
                                        model = it.profile.avatar,
                                        contentDescription = "用户头像",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .rotate(rotation.value)
                                            .clip(CircleShape)
                                            .clickable(interactionSource = MutableInteractionSource(), enabled = state != AudioState.PLAYING) { player.play() },
                                        contentScale = ContentScale.Crop,
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // 用户信息
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = it.profile.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = it.profile.studyName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

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
                headlineContent = {
                    Text("系统日志")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = "系统设置",
                    )
                },
                modifier = Modifier.clickable(onClick = onLogcatClicked)
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
