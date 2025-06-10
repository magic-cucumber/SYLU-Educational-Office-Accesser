import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import io.github.vinceglb.filekit.FileKit
import top.kagg886.eoa.App
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.installCoilConfig
import top.kagg886.util.initializeMMKV

fun main() = application {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }
    initializeMMKV()
    FileKit.init(appId = BuildConfig.APP_VERSION_NAME)
    Window(
        title = BuildConfig.APP_VERSION_NAME,
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        App()
    }
}

