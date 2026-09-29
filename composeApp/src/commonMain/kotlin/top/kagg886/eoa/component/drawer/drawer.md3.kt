package top.kagg886.eoa.component.drawer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.snack.EOAToaster
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.PredictiveBackHandler
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A drawer-sheet page which lives in the current composition instead of opening another window.
 * The drag/animation logic is forked from Material3 [ModalNavigationDrawer] and the sheet styling
 * from [ModalDrawerSheet], with [DrawerSheetPopupDirection] controlling which edge the sheet
 * pops up from.
 */
@Composable
fun DrawerSheetPageScaffold(
    modifier: Modifier = Modifier,
    snack: ToasterState = rememberToasterState(),
    direction: DrawerSheetPopupDirection = DrawerSheetPopupDirection.LEFT,
    content: @Composable DrawerSheetPageScaffoldScope.() -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        val navigation = LocalNavController.current
        val owner = LocalLifecycleOwner.current
        val entry = remember(navigation, owner) {
            owner as? NavBackStackEntry ?: navigation.currentBackStackEntry
        }
        val currentEntry by navigation.currentBackStackEntryAsState()
        val scope = rememberCoroutineScope()
        var closeRequested by remember { mutableStateOf(false) }
        var predictiveBackOffset by remember { mutableStateOf<Float?>(null) }
        var predictiveBackStartOffset by remember { mutableFloatStateOf(0f) }
        var predictiveBackInProgress by remember { mutableStateOf(false) }
        var predictiveBackAnimationJob by remember { mutableStateOf<Job?>(null) }
        fun requestClose(): Boolean {
            if (closeRequested || entry == null || navigation.currentBackStackEntry != entry) return false
            closeRequested = true
            navigation.popBackStack()
            return true
        }

        val draggableState = remember { AnchoredDraggableState(DrawerPosition.Closed) }
        val draggableInteractionSource = remember { MutableInteractionSource() }
        val isDragging by draggableInteractionSource.collectIsDraggedAsState()
        val animationSpec = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = draggableState,
            positionalThreshold = { distance -> distance * 0.5f },
            animationSpec = animationSpec
        )
        var initialTarget by remember { mutableStateOf<DrawerPosition?>(null) }

        LaunchedEffect(initialTarget) {
            initialTarget?.let { draggableState.animateTo(it, animationSpec) }
        }

        // targetValue can change while the pointer is still down. Only dismiss after the
        // Do not dismiss while targetValue changes under the pointer; wait for release.
        LaunchedEffect(draggableState) {
            var hasBeenVisible = false
            snapshotFlow {
                Triple(
                    isDragging || predictiveBackInProgress,
                    draggableState.targetValue,
                    draggableState.settledValue
                )
            }.collect { (dragging, target, settled) ->
                if (settled != DrawerPosition.Closed) hasBeenVisible = true
                if (
                    !dragging &&
                    hasBeenVisible &&
                    target == DrawerPosition.Closed
                ) {
                    requestClose()
                }
            }
        }

        val onClose: () -> Unit = {
            if (
                !predictiveBackInProgress &&
                draggableState.settledValue != DrawerPosition.Closed &&
                requestClose()
            ) {
                scope.launch {
                    draggableState.animateTo(DrawerPosition.Closed, animationSpec)
                }
            }
        }

        fun updatePredictiveBack(progress: Float) {
            val closedOffset = draggableState.anchors.positionOf(DrawerPosition.Closed)
            if (closedOffset.isNaN()) return
            predictiveBackOffset = predictiveBackStartOffset +
                    (closedOffset - predictiveBackStartOffset) * progress.coerceIn(0f, 1f)
        }

        fun finishPredictiveBack(commit: Boolean) {
            if (!predictiveBackInProgress) return
            predictiveBackAnimationJob?.cancel()
            predictiveBackAnimationJob = scope.launch {
                val closedOffset = draggableState.anchors.positionOf(DrawerPosition.Closed)
                val startOffset = predictiveBackOffset ?: draggableState.requireOffset()
                val endOffset = if (commit) closedOffset else draggableState.requireOffset()
                val fullDistance = abs(closedOffset - predictiveBackStartOffset)
                val remainingFraction = if (fullDistance > 0f) {
                    (abs(endOffset - startOffset) / fullDistance).coerceIn(0f, 1f)
                } else 1f
                animate(
                    startOffset,
                    endOffset,
                    animationSpec = tween(
                        durationMillis = (320 * remainingFraction).roundToInt().coerceAtLeast(1),
                        easing = FastOutSlowInEasing
                    )
                ) { value, _ -> predictiveBackOffset = value }
                if (commit) {
                    draggableState.snapTo(DrawerPosition.Closed)
                    // Retain the closed preview until the host removes this destination.
                    requestClose()
                } else {
                    predictiveBackOffset = null
                }
                predictiveBackInProgress = false
            }
        }

        PredictiveBackHandler(
            enabled = draggableState.settledValue != DrawerPosition.Closed &&
                    !closeRequested && entry != null && currentEntry == entry,
            onBackStarted = { event ->
                predictiveBackAnimationJob?.cancel()
                predictiveBackStartOffset = predictiveBackOffset ?: draggableState.requireOffset()
                predictiveBackInProgress = true
                predictiveBackOffset = predictiveBackStartOffset
                updatePredictiveBack(event.progress)
            },
            onBackProgressed = { event -> updatePredictiveBack(event.progress) },
            onBackCancelled = { finishPredictiveBack(commit = false) }
        ) {
            if (predictiveBackInProgress) finishPredictiveBack(commit = true) else onClose()
        }

        val navigationMenu = "导航菜单"
        // ModalDrawerSheet rounds the corners that are not attached to the screen edge.
        // RoundedCornerShape resolves Start/End against the current LayoutDirection by itself.
        val sheetShape = when (direction) {
            DrawerSheetPopupDirection.LEFT -> RoundedCornerShape(
                topEnd = DrawerCornerRadius,
                bottomEnd = DrawerCornerRadius
            )

            DrawerSheetPopupDirection.RIGHT -> RoundedCornerShape(
                topStart = DrawerCornerRadius,
                bottomStart = DrawerCornerRadius
            )
        }
        val sheetAlignment =
            if (direction == DrawerSheetPopupDirection.LEFT) Alignment.CenterStart
            else Alignment.CenterEnd
        val sheetInsetSide =
            if (direction == DrawerSheetPopupDirection.LEFT) WindowInsetsSides.Start
            else WindowInsetsSides.End

        CompositionLocalProvider(LocalSnackBarHost provides snack) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .anchoredDraggable(
                        state = draggableState,
                        enabled = !predictiveBackInProgress && !closeRequested,
                        orientation = Orientation.Horizontal,
                        interactionSource = draggableInteractionSource,
                        flingBehavior = flingBehavior
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) {
                        onClose()
                    },
                contentAlignment = sheetAlignment
            ) {
                val drawerScope = remember { DrawerSheetPageScaffoldScopeImpl(onClose) }
                Surface(
                    modifier = modifier
                        .widthIn(min = MinimumDrawerWidth, max = MaximumDrawerWidth)
                        .fillMaxHeight()
                        .offset {
                            IntOffset(
                                x = (predictiveBackOffset ?: draggableState.offset)
                                    .takeUnless(Float::isNaN)
                                    ?.roundToInt()
                                    // Before the anchors are initialized keep the sheet fully
                                    // off-screen, like the Closed anchor in ModalNavigationDrawer.
                                    ?: if (direction == DrawerSheetPopupDirection.LEFT)
                                        -constraints.maxWidth
                                    else constraints.maxWidth,
                                y = 0
                            )
                        }
                        .onSizeChanged { sheetSize ->
                            val closedAnchor =
                                if (direction == DrawerSheetPopupDirection.LEFT)
                                    -sheetSize.width.toFloat()
                                else sheetSize.width.toFloat()
                            draggableState.updateAnchors(
                                DraggableAnchors {
                                    DrawerPosition.Closed at closedAnchor
                                    DrawerPosition.Open at 0f
                                },
                                draggableState.targetValue
                            )
                            if (initialTarget == null) {
                                initialTarget = DrawerPosition.Open
                            }
                        }
                        .imePadding()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = {}
                        )
                        .semantics { paneTitle = navigationMenu },
                    shape = sheetShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = ModalDrawerElevation
                ) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Vertical + sheetInsetSide
                                )
                            )
                    ) {
                        drawerScope.content()
                    }
                }
            }
        }

        val model = rootViewModel()
        val rootState by model.collectAsState()
        val theme by rootState.theme.collectAsState()
        val dark = theme == AppSettingsMMKVType.AppTheme.Dark ||
                (theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme())

        EOAToaster(
            state = snack,
            dark = dark,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

enum class DrawerSheetPopupDirection {
    LEFT, RIGHT
}

private enum class DrawerPosition {
    Closed,
    Open
}

// Forked from Material3's DrawerDefaults / NavigationDrawerTokens.
private val MinimumDrawerWidth = 240.dp
private val MaximumDrawerWidth = 360.dp
private val DrawerCornerRadius = 16.dp
private val ModalDrawerElevation = 0.dp
