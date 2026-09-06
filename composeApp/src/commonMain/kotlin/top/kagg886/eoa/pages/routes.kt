package top.kagg886.eoa.pages

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import io.ktor.util.encodeBase64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.eoa.pages.captcha.CaptchaRoute
import top.kagg886.eoa.pages.captcha.CaptchaScreen
import top.kagg886.eoa.pages.announcement.AnnouncementRoute
import top.kagg886.eoa.pages.announcement.AnnouncementScreen
import top.kagg886.eoa.pages.logcat.LogcatRoute
import top.kagg886.eoa.pages.logcat.LogcatScreen
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.pages.login.LoginScreen
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.home.HomeRoute
import top.kagg886.eoa.pages.main.installMainGraph
import top.kagg886.eoa.pages.update.UpdateRoute
import top.kagg886.eoa.pages.update.detail.UpdateDetailRoute
import top.kagg886.eoa.pages.update.installUpdateGraph
import top.kagg886.eoa.pages.welcome.WelcomeRoute
import top.kagg886.eoa.pages.welcome.home.WelcomeHomeRoute
import top.kagg886.eoa.pages.welcome.installWelcomeGraph
import kotlin.io.encoding.Base64
import kotlin.reflect.typeOf

@Serializable
data object RootRoute

val installEOAGraph: (NavGraphBuilder.() -> Unit) = {
    navigation<RootRoute>(startDestination = WelcomeRoute) {
        navigation<WelcomeRoute>(startDestination = WelcomeHomeRoute, builder = installWelcomeGraph)
        composable<LoginRoute> { LoginScreen() }
        navigation<UpdateRoute>(startDestination = UpdateDetailRoute("", "", ""), builder = installUpdateGraph)
        dialog<AnnouncementRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
            AnnouncementScreen(
                it.toRoute()
            )
        }
        dialog<CaptchaRoute>(
            typeMap = mapOf(typeOf<ByteArray>() to ByteArrayNavType),
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            CaptchaScreen(it.toRoute())
        }
        navigation<MainRoute>(startDestination = HomeRoute, builder = installMainGraph)
        composable<LogcatRoute> { LogcatScreen() }
    }
}


private data object ByteArrayNavType: NavType<ByteArray>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: ByteArray) {
        bundle.write {
            putString(key, Base64.UrlSafe.encode(value))
        }
    }

    override fun get(
        bundle: SavedState,
        key: String
    ): ByteArray? {
        return bundle.read {
            runCatching { Base64.UrlSafe.decode(getString(key)) }.getOrNull()
        }
    }

    override fun parseValue(value: String): ByteArray {
        return Base64.UrlSafe.decode(value)
    }
    override fun serializeAsValue(value: ByteArray): String {
        return Base64.UrlSafe.encode(value)
    }
}