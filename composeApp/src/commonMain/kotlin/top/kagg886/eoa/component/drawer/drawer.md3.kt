package top.kagg886.eoa.component.drawer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.isSystemInDarkTheme
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
        val scope = rememberCoroutineScope()
        val draggableState = remember { AnchoredDraggableState(DrawerPosition.Closed) }
        val animationSpec = tween<Float>(durationMillis = 320, easing = FastOutSlowInEasing)
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = draggableState,
            positionalThreshold = { distance -> distance * 0.5f },
            animationSpec = animationSpec
        )
        var initialTarget by remember { mutableStateOf<DrawerPosition?>(null) }
        var dismissingFromScrim by remember { mutableStateOf(false) }

        LaunchedEffect(initialTarget) {
            initialTarget?.let { draggableState.animateTo(it, animationSpec) }
        }

        LaunchedEffect(draggableState) {
            var hasBeenVisible = false
            snapshotFlow { draggableState.settledValue }.collect { value ->
                if (value == DrawerPosition.Closed) {
                    if (hasBeenVisible && !dismissingFromScrim) navigation.popBackStack()
                } else {
                    hasBeenVisible = true
                }
            }
        }

        val onClose: () -> Unit = {
            if (draggableState.settledValue != DrawerPosition.Closed) {
                scope.launch {
                    draggableState.animateTo(DrawerPosition.Closed, animationSpec)
                }
            }
        }

        BackHandler(enabled = draggableState.settledValue != DrawerPosition.Closed) {
            onClose()
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
                        orientation = Orientation.Horizontal,
                        flingBehavior = flingBehavior
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) {
                        if (!dismissingFromScrim) {
                            dismissingFromScrim = true
                            scope.launch {
                                try {
                                    draggableState.animateTo(DrawerPosition.Closed, animationSpec)
                                } finally {
                                    navigation.popBackStack()
                                }
                            }
                        }
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
                                x = draggableState.offset
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
