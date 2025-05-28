import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import top.kagg886.eoa.App
import platform.UIKit.UIViewController
import top.kagg886.eoa.installCoilConfig

fun MainViewController(): UIViewController = ComposeUIViewController {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }
    App()
}
