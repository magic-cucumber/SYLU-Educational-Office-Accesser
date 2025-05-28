package top.kagg886.eoa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.util.Logger
import top.kagg886.util.initializeMMKV
import top.kagg886.eoa.pages.installEOAGraph
import top.kagg886.eoa.pages.welcome.WelcomeRoute
import top.kagg886.eoa.theme.AppTheme
import top.kagg886.util.logger

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("not provided")
}

@Composable
internal fun App() = AppTheme {
    LaunchedEffect(Unit) {
        initializeMMKV()
    }
    CompositionLocalProvider(
        LocalNavController provides rememberNavController()
    ) {
        Surface(Modifier.fillMaxSize()) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = LocalNavController.current,
                startDestination = WelcomeRoute,
                builder = installEOAGraph,
            )
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
