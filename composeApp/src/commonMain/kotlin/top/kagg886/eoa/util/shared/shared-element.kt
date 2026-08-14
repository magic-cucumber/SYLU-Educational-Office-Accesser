package top.kagg886.eoa.util.shared

import androidx.compose.animation.*
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.ContentSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection


@OptIn(ExperimentalSharedTransitionApi::class)
val LocalShareTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
    error("LST not provided")
}

val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope> {
    error("LAC not provided")
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
    placeholderSize: PlaceholderSize = ContentSize,
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
            placeholderSize,
            renderInOverlayDuringTransition,
            zIndexInOverlay,
            clipInOverlayDuringTransition
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shareBoundsComposed(
    sharedContentState: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = SharedTransitionDefaults.BoundsTransform,
    resizeMode: SharedTransitionScope.ResizeMode = scaleToBounds(
        ContentScale.FillWidth,
        Alignment.Center
    ),
    placeholderSize: PlaceholderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip,
) = composed {
    val scope = LocalShareTransitionScope.current

    with(scope) {
        sharedBounds(
            sharedContentState,
            animatedVisibilityScope,
            enter,
            exit,
            boundsTransform,
            resizeMode,
            placeholderSize,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OverlayClip(shape: Shape): OverlayClip = LocalShareTransitionScope.current.OverlayClip(shape)
