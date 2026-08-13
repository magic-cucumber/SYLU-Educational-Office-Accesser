package top.kagg886.eoa

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.request.Options
import coil3.util.Logger
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.flow.MutableSharedFlow
import okio.ByteString.Companion.toByteString
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.component.dialog.DialogHost
import top.kagg886.eoa.component.nav.NavHost
import top.kagg886.eoa.component.snack.EOAToaster
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.RootEffect
import top.kagg886.eoa.pages.RootRoute
import top.kagg886.eoa.pages.announcement.AnnouncementRoute
import top.kagg886.eoa.pages.installEOAGraph
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.pages.update.detail.UpdateDetailRoute
import top.kagg886.eoa.pages.update.detail.UpdateInfo
import top.kagg886.eoa.theme.AppTheme
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.handleDeepLink
import top.kagg886.eoa.util.registerKermitLoggerIfExists
import top.kagg886.eoa.util.shared.LocalShareTransitionScope
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.util.*

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("not provided")
}

val LocalSnackBarHost = staticCompositionLocalOf<ToasterState> {
    error("not provided")
}

val LocalGlobalViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("not provided")
}


data class DeeplinkController(
    internal val deepLinkFlow: MutableSharedFlow<String?> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = 16
    ),
) {
    fun handleDeepLink(deepLink: String?) = deepLinkFlow.tryEmit(deepLink)
}

@Composable
fun rememberDeepLinkController(): DeeplinkController = remember {
    DeeplinkController()
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(controller: DeeplinkController = rememberDeepLinkController()) = CompositionLocalProvider(
    LocalGlobalViewModelStoreOwner provides LocalViewModelStoreOwner.current!!,
    LocalNavController provides rememberNavController(),
    LocalSnackBarHost provides rememberToasterState(),
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
                    UpdateDetailRoute(
                        it.data.tag_name,
                        it.data.body.replace("\r", ""),
                        downloadResourceUrl(it.data)
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
        registerKermitLoggerIfExists(rootModel.appLogDao)
        "App.kt".asTaggedLogger.i {
            buildString {
                appendLine("Application Info:")
                appendLine("    Platform: ${Platform.current}")
                appendLine("    Version: ${BuildConfig.APP_VERSION_NAME}(${BuildConfig.APP_VERSION_CODE})")

                with(Platform.current) {
                    if (this is Platform.Android) {
                        appendLine("    Desugaring: $useDesugarApi")
                    }
                }

            }
        }
    }

    // 处理深层链接
    LaunchedEffect(controller) {
        controller.deepLinkFlow.collect { deepLinkUri ->
            if (deepLinkUri != null) {
                nav.handleDeepLink(deepLinkUri)
            }
        }
    }

    val dark =
        (theme == AppSettingsMMKVType.AppTheme.Dark) || (theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme())
    AppTheme(
        color = color,
        nightTheme = dark
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
                            dialogHost = { nav ->
                                DialogHost(dialogNavigator = nav)
                            },
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
            EOAToaster(
                state = LocalSnackBarHost.current,
                dark = dark,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

fun ImageLoader.Builder.installCoilConfig(): ImageLoader.Builder = this
    .logger(
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
    .components {
        add(
            Keyer<ByteArray> { data, _ -> data.toByteString().sha256().hex() }
        )
    }

expect fun downloadResourceUrl(info: UpdateInfo): String
