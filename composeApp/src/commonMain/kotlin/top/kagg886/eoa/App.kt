package top.kagg886.eoa

import StackedSnackbarAnimation
import StackedSnackbarHost
import StackedSnakbarHostState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.util.Logger
import rememberStackedSnackbarHostState
import top.kagg886.util.initializeMMKV
import top.kagg886.eoa.pages.installEOAGraph
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.welcome.WelcomeRoute
import top.kagg886.eoa.theme.AppTheme
import top.kagg886.eoa.util.shared.LocalShareTransitionScope
import top.kagg886.util.logger

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("not provided")
}

val LocalSnackBarHost = staticCompositionLocalOf<StackedSnakbarHostState> {
    error("not provided")
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun App() = AppTheme {
    LaunchedEffect(Unit) {
        initializeMMKV()
    }
    CompositionLocalProvider(
        LocalNavController provides rememberNavController(),
        LocalSnackBarHost provides rememberStackedSnackbarHostState(
            maxStack = 3,
            animation = StackedSnackbarAnimation.Slide
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize()) {
                val nav = LocalNavController.current
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalShareTransitionScope provides this) {
                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = nav,
                            startDestination = WelcomeRoute,
                            builder = installEOAGraph,
                        )
                    }
                }
                val stack by nav.currentBackStack.collectAsState()
                LaunchedEffect(stack) {
                    if (stack.isEmpty()) {
                        //出bug了！速速补救。
                        nav.navigate(WelcomeRoute)
                        return@LaunchedEffect
                    }
                    val flow = stack.joinToString(" -> ") { s -> s.destination.route ?: "root" }
                    logger.i("Route Stack Modified: $flow")

                }
            }

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
