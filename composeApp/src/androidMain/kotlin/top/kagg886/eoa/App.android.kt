package top.kagg886.eoa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.flow.MutableStateFlow
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.update.detail.UpdateInfo
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.initializeMMKV

class AppActivity : ComponentActivity() {
    private val logger = "AppActivity".asTaggedLogger

    private val deepLinkFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeMMKV()
        FileKit.init(this)


        setContent {
            val deepLinkUri by deepLinkFlow.collectAsState(null)
            LaunchedEffect(deepLinkUri) {
                logger.d("发现深层链接：$deepLinkUri")
            }
            App(deepLinkUri = deepLinkUri)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkFlow.tryEmit(intent.data.toString())
    }
}

actual fun downloadResourceUrl(info: UpdateInfo): String =
    info.assets.first { it.name.endsWith(if (BuildConfig.APP_DESUGAR_ENABLED) "-6.apk" else ".apk") }.browser_download_url
