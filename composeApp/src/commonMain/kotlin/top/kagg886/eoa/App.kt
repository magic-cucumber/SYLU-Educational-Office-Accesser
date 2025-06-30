package top.kagg886.eoa

import StackedSnackbarAnimation
import StackedSnackbarHost
import StackedSnakbarHostState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.util.Logger
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import rememberStackedSnackbarHostState
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.pages.RootEffect
import top.kagg886.eoa.pages.RootRoute
import top.kagg886.eoa.pages.announcement.AnnouncementRoute
import top.kagg886.eoa.pages.installEOAGraph
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.pages.update.UpdateRoute
import top.kagg886.eoa.theme.AppTheme
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.shared.LocalShareTransitionScope
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.logger

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("not provided")
}

val LocalSnackBarHost = staticCompositionLocalOf<StackedSnakbarHostState> {
    error("not provided")
}

val LocalGlobalViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("not provided")
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun App() = CompositionLocalProvider(
    LocalGlobalViewModelStoreOwner provides LocalViewModelStoreOwner.current!!,
    LocalNavController provides rememberNavController(),
    LocalSnackBarHost provides rememberStackedSnackbarHostState(
        maxStack = 3,
        animation = StackedSnackbarAnimation.Slide
    ),
) {
    val nav = LocalNavController.current
    val snack = LocalSnackBarHost.current


    val rootModel = rootViewModel()
    rootModel.collectSideEffect {
        when (it) {
            is RootEffect.Toast -> {
                snack.showSnackBar(SnackBarType.Info, it.msg)
            }

            is RootEffect.NavigateToUpdatePage -> {
                nav.navigate(
                    UpdateRoute(
                        it.data.tag_name,
                        it.data.body.replace("\r", ""),
                        "https://gitee.com/kagg886/sylu-educational-office-accesser/releases/latest"
                    )
                )
            }

            is RootEffect.NavigateToAnnouncePage -> {
                nav.navigate(
                    AnnouncementRoute(
                        it.data.replace("\r", "")
                    )
                )
            }
        }
    }

    val rootState by rootModel.collectAsState()
    val color by rootState.color.collectAsState()
    val theme by rootState.theme.collectAsState()

    LaunchedEffect(Unit) {
        co.touchlab.kermit.Logger.addLogWriter(
            object : LogWriter() {
                override fun log(
                    severity: Severity, message: String, tag: String, throwable: Throwable?
                ) {
                    rootModel.log(severity, tag, message, throwable)
                }
            }
        )
    }

    AppTheme(
        color = color,
        nightTheme = (theme == AppSettingsMMKVType.AppTheme.Dark) || (theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme())
    ) {
        Box(Modifier.fillMaxSize()) {
            //业务
            Surface(Modifier.fillMaxSize()) {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalShareTransitionScope provides this) {
                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = nav,
                            startDestination = RootRoute,
                            builder = installEOAGraph,
                        )
                    }
                }


                val stack by nav.currentBackStack.collectAsState()
                LaunchedEffect(stack) {
                    if (stack.isEmpty()) {
                        //出bug了！速速补救。
                        nav.navigate(MainRoute)
                        return@LaunchedEffect
                    }
                    val flow = stack.joinToString(" -> ") { s -> s.destination.route ?: "root" }
                    "App.kt".asTaggedLogger.d("Route Stack Modified: $flow")
                }
            }

            //toaster
            Box(Modifier.align(Alignment.BottomCenter)) {
                StackedSnackbarHost(
                    hostState = LocalSnackBarHost.current,
                )
            }
        }
    }
}

fun ImageLoader.Builder.installCoilConfig(): ImageLoader.Builder = this.logger(
    object : Logger {
        override var minLevel: Logger.Level = Logger.Level.Verbose
        override fun log(
            tag: String,
            level: Logger.Level,
            message: String?,
            throwable: Throwable?
        ) = logger.log(Severity.valueOf(level.name), "Coil - $tag", throwable, message ?: "")
    }
)
