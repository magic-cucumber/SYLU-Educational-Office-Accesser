package top.kagg886.eoa.pages.welcome

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.welcome.home.WelcomeHomeRoute
import top.kagg886.eoa.pages.welcome.home.WelcomeHomeScreen
import top.kagg886.eoa.pages.welcome.privacy.WelcomePrivacyRoute
import top.kagg886.eoa.pages.welcome.privacy.WelcomePrivacyScreen
import top.kagg886.eoa.pages.welcome.done.WelcomeDoneRoute
import top.kagg886.eoa.pages.welcome.done.WelcomeDoneScreen
import top.kagg886.eoa.pages.welcome.theme.WelcomeThemeRoute
import top.kagg886.eoa.pages.welcome.theme.WelcomeThemeScreen
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType


@Serializable
data object WelcomeRoute

val installWelcomeGraph: NavGraphBuilder.() -> Unit = {
    composable<WelcomeHomeRoute> { WelcomeHomeScreen() }
    composable<WelcomeThemeRoute> { WelcomeThemeScreen() }
    composable<WelcomePrivacyRoute> { WelcomePrivacyScreen() }
    composable<WelcomeDoneRoute> { WelcomeDoneScreen() }
}
