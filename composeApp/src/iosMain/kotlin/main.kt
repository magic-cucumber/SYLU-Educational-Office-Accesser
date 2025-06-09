import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import top.kagg886.eoa.App
import platform.UIKit.UIViewController
import top.kagg886.eoa.installCoilConfig
import top.kagg886.util.initializeMMKV

fun MainViewController(): UIViewController = ComposeUIViewController {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }

    initializeMMKV()
    App()
}
