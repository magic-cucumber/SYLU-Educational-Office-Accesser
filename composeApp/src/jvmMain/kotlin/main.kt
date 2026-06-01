import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import org.jetbrains.compose.resources.painterResource
import sylu_eoa.composeapp.generated.resources.Res
import sylu_eoa.composeapp.generated.resources.icon
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.eoa.App
import top.kagg886.eoa.installCoilConfig
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.initializeMMKV
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val logger = "DesktopMain".asTaggedLogger

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        logger.a(throwable) { "App crashed on thread ${thread.name}" }
    }

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }
    initializeMMKV()
    FileKit.init(appId = "SYLU-EOA")
    CompositionLocalProvider(
        LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory {
            WindowExceptionHandler { throwable ->
                logger.a(throwable) { "App crashed on main thread" }
                exitApplication()
            }
        },
    ) {
        val state = rememberWindowState(
            width = AppInitializeMMKV.size.first.dp,
            height = AppInitializeMMKV.size.second.dp,
            position = when (val offset = AppInitializeMMKV.offset) {
                null -> WindowPosition.PlatformDefault
                else -> WindowPosition.Absolute(offset.first.dp, offset.second.dp)
            }
        )

        LaunchedEffect(Unit) {
            logger.i("App size: ${state.size.width} x ${state.size.height}, offset = ${state.position}")
        }

        LaunchedEffect(state) {
            @OptIn(FlowPreview::class)
            snapshotFlow { state.size }.distinctUntilChanged().sample(1.seconds).collect { (w,h) ->
                logger.d("desktop window size changed: $w, $h")
                AppInitializeMMKV.size = w.value.roundToInt() to h.value.roundToInt()
            }
        }

        LaunchedEffect(state) {
            @OptIn(FlowPreview::class)
            snapshotFlow { state.position }.distinctUntilChanged().sample(1.seconds).collect { position ->
                if (position is WindowPosition.PlatformDefault) {
                    AppInitializeMMKV.offset = null
                    return@collect
                }
                AppInitializeMMKV.offset = with(position) {
                    logger.d("desktop window offset changed: $x, $y")
                    x.value.roundToInt() to y.value.roundToInt()
                }
            }
        }

        Window(
            title = "SYLU-EOA",
            state = state,
            icon = painterResource(Res.drawable.icon),
            onCloseRequest = ::exitApplication,
            content = { App() }
        )
    }
}
