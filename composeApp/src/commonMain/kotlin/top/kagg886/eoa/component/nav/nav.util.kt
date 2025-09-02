package top.kagg886.eoa.component.nav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/3 11:16
 * ================================================
 */

public inline fun <reified T : Any> NavGraphBuilder.transition(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
): Unit = composable<T>(
    enterTransition = { fadeIn() },
    exitTransition = { fadeOut() },
    popEnterTransition = { fadeIn() },
    popExitTransition = { fadeOut() }
) {
    CompositionLocalProvider(
        LocalAnimatedContentScope provides this,
        content = { content(it) }
    )
}
