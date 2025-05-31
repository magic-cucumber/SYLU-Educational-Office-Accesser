package top.kagg886.eoa.util.shared

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection


@OptIn(ExperimentalSharedTransitionApi::class)
val LocalShareTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
    error("LST not provided")
}

val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope> {
    error("LAC not provided")
}

@Composable
fun AnimatedContentScope.AutoInject(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAnimatedContentScope provides this,
        content = content
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun rememberSharedContentState(key: Any): SharedContentState {
    val scope = LocalShareTransitionScope.current
    return with(scope) {
        rememberSharedContentState(key = key)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shareElementComposed(
    sharedContentState: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
) = composed {
    val scope = LocalShareTransitionScope.current

    with(scope) {
        sharedElement(
            sharedContentState,
            animatedVisibilityScope,
            boundsTransform,
            placeHolderSize,
            renderInOverlayDuringTransition,
            zIndexInOverlay,
            clipInOverlayDuringTransition
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val DefaultBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold
    )
}

@ExperimentalSharedTransitionApi
private val ParentClip: OverlayClip =
    object : OverlayClip {
        override fun getClipPath(
            sharedContentState: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return sharedContentState.parentSharedContentState?.clipPathInOverlay
        }
    }

