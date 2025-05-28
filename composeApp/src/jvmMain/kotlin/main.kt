import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import java.awt.Dimension
import top.kagg886.eoa.App
import top.kagg886.eoa.installCoilConfig

fun main() = application {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }
    Window(
        title = "SYLU-EOA",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(800, 600)
        App()
    }
}

