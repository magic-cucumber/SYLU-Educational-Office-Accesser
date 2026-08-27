package top.kagg886.eoa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.flow.MutableStateFlow
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.eoa.util.longshot.setContent
import top.kagg886.util.initializeMMKV

class AppActivity : ComponentActivity() {
    private val deepLinkFlow = MutableStateFlow<String?>(null)

    @OptIn(InternalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeMMKV()
        FileKit.init(this)

        WindowRecomposerPolicy.setFactory { rootView ->
            rootView.createLifecycleAwareWindowRecomposer(
                lifecycle = this.lifecycle,
                coroutineContext = object : MotionDurationScale {
                    override val scaleFactor: Float
                        get() = 1 / AppSettingsMMKV.animationSpeed

                }
            )
        }

        val application = this.application as EOAApplication

        setContent {
            val controller = rememberDeepLinkController()
            LaunchedEffect(deepLinkFlow) {
                deepLinkFlow.collect(controller::handleDeepLink)
            }
            CompositionLocalProvider(LocalDatabase provides application.database) {
                App(controller)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkFlow.tryEmit(intent.data.toString())
    }
}
