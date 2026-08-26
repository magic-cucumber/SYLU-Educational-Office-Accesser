package top.kagg886.eoa.component.bottomsheet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.snack.EOAToaster
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.BackHandler
import kotlin.math.max
import kotlin.math.roundToInt


/**
 * A bottom-sheet page which lives in the current composition instead of opening another window.
 * Its drag state is deliberately kept private so callers only provide static page content.
 *
 * @param initialPopupType the position the sheet animates to when first shown.
 * Must not be [SheetPosition.Hidden]; if [SheetPosition.PartiallyExpanded] has no anchor
 * (sheet taller than half the screen), it falls back to [SheetPosition.Expanded].
 * @param popupTypeChangeRequest decides which positions the sheet may settle at:
 * positions for which it returns `false` are excluded from the drag anchors,
 * so neither gestures nor programmatic animations can reach them.
 * It must at least allow [SheetPosition.Expanded].
 */
@Composable
fun BottomSheetPageScaffold(
    modifier: Modifier = Modifier,
    snack: ToasterState = rememberToasterState(),
    maxExpandedHeight: Dp = Dp.Unspecified,
    initialPopupType: SheetPosition = SheetPosition.PartiallyExpanded,
    popupTypeChangeRequest: (SheetPosition) -> Boolean = { true },
    content: @Composable BottomSheetPageScaffoldScope.() -> Unit = {}
) {
    require(maxExpandedHeight == Dp.Unspecified || maxExpandedHeight > 0.dp) {
        "maxExpandedHeight must be positive or Dp.Unspecified."
    }
    require(initialPopupType != SheetPosition.Hidden) {
        "initialPopupType must not be SheetPosition.Hidden."
    }
    require(popupTypeChangeRequest(SheetPosition.Expanded)) {
        "popupTypeChangeRequest must allow SheetPosition.Expanded."
    }

    Box(Modifier.fillMaxSize()) {
        val navigation = LocalNavController.current
        val scope = rememberCoroutineScope()
        val animationSpec = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
        val sheetShape =
            RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)
        val allowProgrammaticTransition = remember { mutableStateOf(false) }
        var closeRequested by remember { mutableStateOf(false) }
        val popupTypeChangeRequestState = rememberUpdatedState(popupTypeChangeRequest)
        lateinit var draggableState: AnchoredDraggableState<SheetPosition>

        fun requestRouteDismiss(): Boolean {
            if (closeRequested) return false
            closeRequested = true
            navigation.popBackStack()
            return true
        }

        fun onClose(): Boolean {
            if (closeRequested) return false

            val target = when (draggableState.settledValue) {
                SheetPosition.Expanded -> when {
                    draggableState.anchors.hasPositionFor(SheetPosition.PartiallyExpanded) ->
                        SheetPosition.PartiallyExpanded

                    draggableState.anchors.hasPositionFor(SheetPosition.Hidden) ->
                        SheetPosition.Hidden

                    else -> null
                }

                SheetPosition.PartiallyExpanded ->
                    if (draggableState.anchors.hasPositionFor(SheetPosition.Hidden)) {
                        SheetPosition.Hidden
                    } else {
                        null
                    }

                SheetPosition.Hidden -> null
            }
            target ?: return false
            if (!popupTypeChangeRequestState.value(target)) return false

            if (target == SheetPosition.Hidden) {
                requestRouteDismiss()
            }

            scope.launch {
                allowProgrammaticTransition.value = true
                try {
                    draggableState.animateTo(target, animationSpec)
                } finally {
                    allowProgrammaticTransition.value = false
                }
            }
            return true
        }

        @Suppress("DEPRECATION")
        val state = remember {
            AnchoredDraggableState(
                initialValue = SheetPosition.Hidden,
                confirmValueChange = { target ->
                    if (allowProgrammaticTransition.value) {
                        true
                    } else {
                        val current = draggableState.settledValue
                        val isClosing = when (current) {
                            SheetPosition.Expanded ->
                                target == SheetPosition.PartiallyExpanded ||
                                        target == SheetPosition.Hidden

                            SheetPosition.PartiallyExpanded -> target == SheetPosition.Hidden
                            SheetPosition.Hidden -> false
                        }
                        if (isClosing) popupTypeChangeRequestState.value(target) else true
                    }
                }
            )
        }
        draggableState = state
        val draggableInteractionSource = remember { MutableInteractionSource() }
        val isDragging by draggableInteractionSource.collectIsDraggedAsState()
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = draggableState,
            positionalThreshold = { distance -> distance * 0.5f },
            animationSpec = animationSpec
        )
        var initialTarget by remember { mutableStateOf<SheetPosition?>(null) }

        LaunchedEffect(initialTarget) {
            initialTarget?.let { draggableState.animateTo(it, animationSpec) }
        }

        // targetValue can change while the pointer is still down. Only dismiss after the
        // Do not dismiss while targetValue changes under the pointer; wait for release.
        LaunchedEffect(draggableState) {
            var hasBeenVisible = false
            snapshotFlow {
                Triple(
                    isDragging,
                    draggableState.targetValue,
                    draggableState.settledValue
                )
            }.collect { (dragging, target, settled) ->
                if (settled != SheetPosition.Hidden) hasBeenVisible = true
                if (
                    !dragging &&
                    hasBeenVisible &&
                    target == SheetPosition.Hidden
                ) {
                    requestRouteDismiss()
                }
            }
        }

        BackHandler(enabled = draggableState.settledValue != SheetPosition.Hidden) {
            onClose()
        }

        CompositionLocalProvider(LocalSnackBarHost provides snack) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onClose() }
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                val fullHeightPx = constraints.maxHeight.toFloat()
                val density = LocalDensity.current
                val sheetMaxHeight =
                    if (maxExpandedHeight != Dp.Unspecified) minOf(
                        maxHeight,
                        maxExpandedHeight
                    ) else maxHeight
                val sheetMaxHeightPx = with(density) { sheetMaxHeight.toPx() }
                val dragHandleHeightPx = with(density) { DragHandleContainerHeight.toPx() }
                val bottomInsetPx = WindowInsets.safeDrawing.getBottom(density).toFloat()
                val initialContentHeight = (
                        minOf(fullHeightPx / 2f, sheetMaxHeightPx) -
                                dragHandleHeightPx -
                                bottomInsetPx
                        ).roundToInt().coerceAtLeast(0)
                val sheetScope = remember(
                    draggableState,
                    fullHeightPx,
                    dragHandleHeightPx,
                    bottomInsetPx,
                    initialContentHeight
                ) {
                    BottomSheetPageScaffoldScopeImpl(
                        minimumContentHeight = initialContentHeight,
                        visibleContentHeight = {
                            val sheetOffset = draggableState.offset
                                .takeUnless(Float::isNaN)
                                ?: fullHeightPx
                            (fullHeightPx - sheetOffset - dragHandleHeightPx - bottomInsetPx)
                                .roundToInt()
                                .coerceAtLeast(0)
                        },
                        onClose = { onClose() },
                    )
                }

                Surface(
                    modifier = modifier
                        .widthIn(max = SheetMaxWidth)
                        .fillMaxWidth()
                        .height(sheetMaxHeight)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = draggableState.offset
                                    .takeUnless(Float::isNaN)
                                    ?.roundToInt()
                                    ?: constraints.maxHeight
                            )
                        }
                        .onSizeChanged { sheetSize ->
                            val expandedOffset = max(0f, fullHeightPx - sheetSize.height)
                            val hasPartialAnchor = sheetSize.height > fullHeightPx / 2f
                            val anchors = DraggableAnchors {
                                if (popupTypeChangeRequest(SheetPosition.Hidden)) {
                                    SheetPosition.Hidden at fullHeightPx
                                }
                                if (hasPartialAnchor &&
                                    popupTypeChangeRequest(SheetPosition.PartiallyExpanded)
                                ) {
                                    SheetPosition.PartiallyExpanded at fullHeightPx / 2f
                                }
                                SheetPosition.Expanded at expandedOffset
                            }
                            val target = when {
                                anchors.hasPositionFor(draggableState.targetValue) ->
                                    draggableState.targetValue

                                anchors.hasPositionFor(SheetPosition.Expanded) ->
                                    SheetPosition.Expanded

                                anchors.hasPositionFor(SheetPosition.PartiallyExpanded) ->
                                    SheetPosition.PartiallyExpanded

                                else -> SheetPosition.Hidden
                            }
                            draggableState.updateAnchors(anchors, target)
                            if (initialTarget == null) {
                                initialTarget = when (initialPopupType) {
                                    SheetPosition.Hidden -> error("unreachable")
                                    SheetPosition.PartiallyExpanded -> {
                                        if (anchors.hasPositionFor(SheetPosition.PartiallyExpanded)) {
                                            SheetPosition.PartiallyExpanded
                                        } else {
                                            SheetPosition.Expanded
                                        }
                                    }

                                    SheetPosition.Expanded -> SheetPosition.Expanded
                                }
                            }
                        }
                        .imePadding()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = {}
                        ),
                    shape = sheetShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .anchoredDraggable(
                                    state = draggableState,
                                    orientation = Orientation.Vertical,
                                    interactionSource = draggableInteractionSource,
                                    flingBehavior = flingBehavior
                                )
                                .clickable(
                                    interactionSource = null,
                                    indication = null
                                ) {
                                    onClose()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .width(32.dp)
                                    .height(4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        sheetScope.content()
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

enum class SheetPosition {
    Hidden,
    PartiallyExpanded,
    Expanded
}

private val SheetMaxWidth = 640.dp
private val DragHandleContainerHeight = 24.dp
private val SheetCornerRadius = 28.dp
