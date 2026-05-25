package top.kagg886.eoa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.withIndex
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.update.detail.UpdateInfo
import top.kagg886.eoa.util.longshot.setContent
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.initializeMMKV

class AppActivity : ComponentActivity() {

    private val deepLinkFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeMMKV()
        FileKit.init(this)

        setContent {
            val controller = rememberDeepLinkController()

            LaunchedEffect(deepLinkFlow) {
                deepLinkFlow.collect { v ->
                    controller.handleDeepLink(v)
                }
            }
            App(controller)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkFlow.tryEmit(intent.data.toString())
    }
}

actual fun downloadResourceUrl(info: UpdateInfo): String =
    info.assets.first { it.name == if (BuildConfig.APP_DESUGAR_ENABLED) "app-release-6.apk" else "app-release.apk" }.browser_download_url
