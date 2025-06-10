import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.painterResource
import sylu_eoa.composeapp.generated.resources.Res
import sylu_eoa.composeapp.generated.resources.icon
import top.kagg886.eoa.App
import top.kagg886.eoa.installCoilConfig
import top.kagg886.util.initializeMMKV

fun main() = application {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }
    initializeMMKV()
    FileKit.init(appId = "SYLU-EOA")
    Window(
        title = "SYLU-EOA",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        icon = painterResource(Res.drawable.icon),
        onCloseRequest = ::exitApplication,
    ) {
        App()
    }
}

