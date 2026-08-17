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

private const val TRANSITION_DURATION_MS = 400

private inline fun <reified T : Any> NavGraphBuilder.animation(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
): Unit = composable<T>(
    typeMap = typeMap,
    deepLinks = deepLinks,
    enterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
        ) + fadeIn(
            animationSpec = tween(TRANSITION_DURATION_MS, easing = LinearOutSlowInEasing),
        )
    },
    exitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffset = { fullSlide -> fullSlide / 4 },
        ) + fadeOut(
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutLinearInEasing),
        )
    },
    popEnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffset = { fullSlide -> fullSlide / 4 },
        ) + fadeIn(
            animationSpec = tween(TRANSITION_DURATION_MS, easing = LinearOutSlowInEasing),
        )
    },
    popExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
        ) + fadeOut(
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutLinearInEasing),
        )
    },
    content = content,
)

val installWelcomeGraph: NavGraphBuilder.() -> Unit = {
    animation<WelcomeHomeRoute> { WelcomeHomeScreen() }
    animation<WelcomeThemeRoute> { WelcomeThemeScreen() }
    animation<WelcomePrivacyRoute> { WelcomePrivacyScreen() }
    animation<WelcomeDoneRoute> { WelcomeDoneScreen() }
}
