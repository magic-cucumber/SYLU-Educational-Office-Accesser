@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.component.bottomsheet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.isSystemInDarkTheme
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

    Box(Modifier.fillMaxSize()) {
        val navigation = LocalNavController.current
        val scope = rememberCoroutineScope()
        val draggableState = remember { AnchoredDraggableState(SheetPosition.Hidden) }
        val animationSpec = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
        val sheetShape =
            RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = draggableState,
            positionalThreshold = { distance -> distance * 0.5f },
            animationSpec = animationSpec
        )
        var initialTarget by remember { mutableStateOf<SheetPosition?>(null) }
        var dismissingFromScrim by remember { mutableStateOf(false) }

        LaunchedEffect(initialTarget) {
            initialTarget?.let { draggableState.animateTo(it, animationSpec) }
        }

        LaunchedEffect(draggableState) {
            var hasBeenVisible = false
            snapshotFlow { draggableState.settledValue }.collect { value ->
                if (value == SheetPosition.Hidden) {
                    if (hasBeenVisible && !dismissingFromScrim) navigation.popBackStack()
                } else {
                    hasBeenVisible = true
                }
            }
        }

        val onClose: () -> Unit = {
            val target = when (draggableState.settledValue) {
                SheetPosition.Expanded -> {
                    if (draggableState.anchors.hasPositionFor(SheetPosition.PartiallyExpanded)) {
                        SheetPosition.PartiallyExpanded
                    } else {
                        SheetPosition.Hidden
                    }
                }

                SheetPosition.PartiallyExpanded -> SheetPosition.Hidden
                SheetPosition.Hidden -> null
            }
            target?.let {
                scope.launch {
                    draggableState.animateTo(it, animationSpec)
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
                        indication = null
                    ) {
                        if (!dismissingFromScrim) {
                            dismissingFromScrim = true
                            scope.launch {
                                try {
                                    if (draggableState.anchors.hasPositionFor(SheetPosition.Hidden)) {
                                        draggableState.animateTo(
                                            SheetPosition.Hidden,
                                            animationSpec
                                        )
                                    }
                                } finally {
                                    navigation.popBackStack()
                                }
                            }
                        }
                    },
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
                        onClose = onClose,
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
                                SheetPosition.Hidden at fullHeightPx
                                if (hasPartialAnchor) {
                                    SheetPosition.PartiallyExpanded at fullHeightPx / 2f
                                }
                                SheetPosition.Expanded at expandedOffset
                            }
                            val target = when (draggableState.targetValue) {
                                SheetPosition.Hidden -> SheetPosition.Hidden
                                SheetPosition.PartiallyExpanded -> {
                                    if (hasPartialAnchor) SheetPosition.PartiallyExpanded
                                    else SheetPosition.Expanded
                                }

                                SheetPosition.Expanded -> SheetPosition.Expanded
                            }
                            draggableState.updateAnchors(anchors, target)
                            if (initialTarget == null) {
                                initialTarget = if (hasPartialAnchor) {
                                    SheetPosition.PartiallyExpanded
                                } else {
                                    SheetPosition.Expanded
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
                                    flingBehavior = flingBehavior
                                )
                                .clickable(
                                    interactionSource = null,
                                    indication = null
                                ) {
                                    val target = when (draggableState.settledValue) {
                                        SheetPosition.Expanded -> {
                                            if (draggableState.anchors.hasPositionFor(
                                                    SheetPosition.PartiallyExpanded
                                                )
                                            ) {
                                                SheetPosition.PartiallyExpanded
                                            } else {
                                                SheetPosition.Hidden
                                            }
                                        }

                                        SheetPosition.PartiallyExpanded -> SheetPosition.Hidden
                                        SheetPosition.Hidden -> return@clickable
                                    }
                                    scope.launch {
                                        draggableState.animateTo(target, animationSpec)
                                    }
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
