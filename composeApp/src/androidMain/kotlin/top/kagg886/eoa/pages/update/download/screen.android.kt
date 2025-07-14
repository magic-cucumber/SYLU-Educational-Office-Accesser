package top.kagg886.eoa.pages.update.download

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Clock
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.absolutePath

@Composable
actual fun UpdateDownloadScreen(route: UpdateDownloadRoute) {
    val model = viewModel(key = route.url) {
        DownloadModel(route.url)
    }

    val state by model.collectAsState()
    val nav = LocalNavController.current
    val snack = LocalSnackBarHost.current
    model.collectSideEffect {
        when (it) {
            is DownloadSideEffect.NavigateBack -> {
                snack.showSnackBar(
                    it.type,
                    it.msg
                )
                nav.popBackStack()
            }
        }
    }

    val ctx = LocalContext.current
    var canInstall by remember(ctx) {
        mutableStateOf(ctx.packageManager.canRequestPackageInstalls())
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        canInstall = ctx.packageManager.canRequestPackageInstalls()
    }
    var ran by remember {
        mutableLongStateOf(-1L)
    }
    if (state is DownloadState.Success) {
        LaunchedEffect(canInstall, ran) {
            if (canInstall) {
                val uri = FileProvider.getUriForFile(
                    ctx, "${ctx.packageName}.fileprovider",
                    (state as DownloadState.Success).path.absolutePath().toFile()
                )

                ctx.findActivity()?.let {
                    it.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        setDataAndType(uri, "application/vnd.android.package-archive")
                    })
                }
                return@LaunchedEffect
            }
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                this.data = "package:${ctx.packageName}".toUri()
            }
            launcher.launch(intent)
        }
    }

    DialogPageScaffold(
        confirmButton = {
            UpdateDownloadConfirmButton(state) {
                ran = Clock.System.now().toEpochMilliseconds()
            }
        },
        dismissButton = {
            val nav = LocalNavController.current
            TextButton(
                onClick = nav::popBackStack
            ) {
                Text("取消")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "下载更新"
            )
        },
        title = {
            Text("下载更新")
        },
        text = {
            UpdateDownloadContent(state) {
                ran = Clock.System.now().toEpochMilliseconds()
            }
        }
    )

}

@Composable
private fun UpdateDownloadConfirmButton(state: DownloadState, onPrepareInstall: () -> Unit) = when (state) {
    DownloadState.Fetching -> {

    }

    is DownloadState.Progressing -> {

    }

    is DownloadState.Success -> {
        TextButton(
            onClick = onPrepareInstall
        ) {
            Text("安装")
        }
    }
}

@Composable
private fun UpdateDownloadContent(state: DownloadState, onPrepareInstall: () -> Unit) = when (state) {
    DownloadState.Fetching -> Box(
        Modifier
            .fillMaxWidth(0.8f),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    is DownloadState.Progressing -> Box(
        Modifier
            .fillMaxWidth(0.8f),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(progress = { state.progress })
    }

    is DownloadState.Success -> {
        LaunchedEffect(Unit) {
            onPrepareInstall()
        }
        Text("等待安装中...")
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
