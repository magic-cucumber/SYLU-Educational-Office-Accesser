package top.kagg886.eoa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.flow.MutableStateFlow
import top.kagg886.eoa.util.longshot.setContent
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
                deepLinkFlow.collect(controller::handleDeepLink)
            }
            App(controller)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkFlow.tryEmit(intent.data.toString())
    }
}
