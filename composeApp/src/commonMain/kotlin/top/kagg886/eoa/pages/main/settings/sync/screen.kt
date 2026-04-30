@file:OptIn(ExperimentalTime::class)

package top.kagg886.eoa.pages.main.settings.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@Serializable
data object SyncSettingsRoute

@Composable
fun SyncSettingsScreen() {
    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val syncDuration by rootState.syncDuration.collectAsState()

    val model = mainViewModel()
    val modelState by model.collectAsState()

    SyncSettingsContent(
        state = modelState,
        syncDuration = syncDuration,
        onSyncDurationChanged = rootModel::postSyncTimeSetting,
        onSyncActionStarted = model::startSyncForce,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSettingsContent(
    state: MainRouteViewState,
    syncDuration: Duration,
    onSyncDurationChanged: (Duration) -> Job,
    onSyncActionStarted: () -> Job,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("同步设置") },
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
            var dialog by remember {
                mutableStateOf(false)
            }
            val snack = LocalSnackBarHost.current
            if (dialog) {
                AlertDialog(
                    onDismissRequest = {
                        dialog = false
                        snack.showSnackBar(SnackBarType.Warning, "同步设置将会在重启后生效")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                dialog = false
                                snack.showSnackBar(SnackBarType.Warning, "同步设置将会在重启后生效")
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    title = {
                        Text("时间选择: ${syncDuration.inWholeDays}天")
                    },
                    text = {
                        Slider(
                            value = syncDuration.inWholeDays.toFloat(),
                            valueRange = 1f..30f,
                            onValueChange = {
                                println(it)
                                onSyncDurationChanged(it.toInt().days)
                            },
                            steps = 28
                        )
                    }
                )
            }

            ListItem(
                headlineContent = {
                    Text("同步时长")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.ViewTimeline,
                        contentDescription = "同步时长",
                    )
                },
                supportingContent = {
                    when (state) {
                        is MainRouteViewState.SyncProcess -> Text("正在同步中，请稍等")

                        is MainRouteViewState.SyncSuccess -> {
                            val time by produceState<Duration?>(null) {
                                while (true) {
                                    value =
                                        (state.lastUpdateTime + syncDuration) - Clock.System.now()
                                    delay(1.hours)
                                }
                            }
                            time?.let {
                                if (it.isNegative()) {
                                    Text("程序将会在下次启动时同步")
                                    return@ListItem
                                }
                                Text("距离下次同步还有${it.toComponents { days, hours, _, _, _ -> "${days}天${hours}时" }}")
                            }
                        }
                        else -> Text("同步成功后，下一次同步会延长多少天")
                    }
                },
                modifier = Modifier.clickable {
                    dialog = true
                }
            )

            val syncButtonEnabled = state is MainRouteViewState.SyncSuccess || state is MainRouteViewState.SyncFailed
            val syncButtonText = when (state) {
                is MainRouteViewState.Empty -> "初始化中"
                is MainRouteViewState.SyncProcess -> "正在同步"
                else -> "立即同步"
            }
            val syncButtonHint = when (state) {
                MainRouteViewState.Empty -> "初始化中"
                is MainRouteViewState.SyncFailed -> state.message
                is MainRouteViewState.SyncProcess -> "请稍等片刻..."
                is MainRouteViewState.SyncSuccess -> "强制进行同步以跟进教务的最新更改。\n仅在确认教务课表变更后本地没有加载才能运行。"
            }
            val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            val disabledSupportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

            ListItem(
                colors = if (syncButtonEnabled) {
                    ListItemDefaults.colors()
                } else {
                    ListItemDefaults.colors(
                        headlineColor = disabledContentColor,
                        leadingIconColor = disabledContentColor,
                        supportingColor = disabledSupportingColor,
                    )
                },
                headlineContent = {
                    Text(syncButtonText)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "立即同步",
                    )
                },
                supportingContent = {
                    Text(syncButtonHint)
                },
                modifier = Modifier.clickable(
                    enabled = syncButtonEnabled,
                    onClick = { onSyncActionStarted() }
                )
            )
        }
    }
}
