@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.component.dialog

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/2 15:56
 * ================================================
 */
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.DialogNavigator.Destination
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.compose.NavHost
import top.kagg886.eoa.util.BackHandler
import top.kagg886.util.asTaggedLogger


/**
 * Show each [Destination] on the [DialogNavigator]'s back stack as a [Dialog].
 *
 * Note that [NavHost] will call this for you; you do not need to call it manually.
 */

private val logger = "DialogHost".asTaggedLogger
@Composable
public fun DialogHost(
    dialogNavigator: DialogNavigator
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val dialogBackStack by dialogNavigator.backStack.collectAsState()
    val visibleBackStack = rememberVisibleList(dialogBackStack)
    visibleBackStack.PopulateVisibleList(dialogBackStack)

    val transitionInProgress by dialogNavigator.transitionInProgress.collectAsState()
    val dialogsToDispose = remember { mutableStateListOf<NavBackStackEntry>() }

    // Maintain rendering entries and their transition states so exit animations can play
    val renderingEntries = remember { mutableStateListOf<NavBackStackEntry>() }
    val entryTransitionStates = remember { mutableStateMapOf<NavBackStackEntry, MutableTransitionState<Boolean>>() }

    // Sync renderingEntries with visibleBackStack: add new, mark invisible for removed
    LaunchedEffect(visibleBackStack) {
        // Add or show entries that are visible
        visibleBackStack.forEach { entry ->
            if (!renderingEntries.contains(entry)) {
                renderingEntries.add(entry)
                entryTransitionStates[entry] = MutableTransitionState(false).apply {
                    targetState = true
                }
                logger.d("Adding $entry to visible list")
            } else {
                entryTransitionStates[entry]?.targetState = true
                logger.d("Showing $entry")
            }
        }
        // Mark entries not currently visible to start exit animation
        renderingEntries.filter { it !in visibleBackStack }.forEach { entry ->
            entryTransitionStates[entry]?.targetState = false
            logger.d("Hiding $entry")
        }
    }

    renderingEntries.forEach { backStackEntry ->
        val destination = backStackEntry.destination as Destination
        val transitionState = remember(backStackEntry) {
            entryTransitionStates.getValue(backStackEntry)
        }

        DisposableEffect(backStackEntry) {
            dialogsToDispose.add(backStackEntry)
            onDispose {
                dialogNavigator.onTransitionComplete(backStackEntry)
                dialogsToDispose.remove(backStackEntry)
            }
        }

        // Remove the entry only after its exit animation finishes
        LaunchedEffect(transitionState) {
            snapshotFlow { transitionState.isIdle to transitionState.currentState }.collect { (idle, current) ->
                if (idle && !current) {
                    logger.d("Removing $backStackEntry from visible list")
                    renderingEntries.remove(backStackEntry)
                    entryTransitionStates.remove(backStackEntry)
                }
            }
        }

        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 320))
        ) {
            BackHandler(destination.dialogProperties.dismissOnBackPress) {
                dialogNavigator.dismiss(backStackEntry)
            }

            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.618f))
                    .clickable(
                        enabled = destination.dialogProperties.dismissOnClickOutside,
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            dialogNavigator.dismiss(backStackEntry)
                        }
                    )
            ) {
                backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                    destination.content.invoke(backStackEntry)
                }
            }
        }
    }
    // Dialogs may have been popped before it was composed. To prevent leakage, we need to
    // mark popped entries as complete here. Check that we don't accidentally complete popped
    // entries that were composed, unless they were disposed of already.
    LaunchedEffect(transitionInProgress, dialogsToDispose) {
        transitionInProgress.forEach { entry ->
            if (
                !dialogNavigator.backStack.value.contains(entry) &&
                !dialogsToDispose.contains(entry)
            ) {
                dialogNavigator.onTransitionComplete(entry)
            }
        }
    }
}

@Composable
internal fun MutableList<NavBackStackEntry>.PopulateVisibleList(
    backStack: Collection<NavBackStackEntry>
) {
    val isInspecting = LocalInspectionMode.current
    backStack.forEach { entry ->
        DisposableEffect(entry.lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                // show dialog in preview
                if (isInspecting && !contains(entry)) {
                    add(entry)
                }
                // ON_START -> add to visibleBackStack, ON_STOP -> remove from visibleBackStack
                if (event == Lifecycle.Event.ON_START) {
                    // We want to treat the visible lists as Sets but we want to keep
                    // the functionality of mutableStateListOf() so that we recompose in response
                    // to adds and removes.
                    if (!contains(entry)) {
                        add(entry)
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    remove(entry)
                }
            }
            entry.lifecycle.addObserver(observer)
            onDispose { entry.lifecycle.removeObserver(observer) }
        }
    }
}

@Composable
internal fun rememberVisibleList(
    backStack: Collection<NavBackStackEntry>
): SnapshotStateList<NavBackStackEntry> {
    // show dialog in preview
    val isInspecting = LocalInspectionMode.current
    return remember(backStack) {
        mutableStateListOf<NavBackStackEntry>().also {
            it.addAll(
                backStack.filter { entry ->
                    if (isInspecting) {
                        true
                    } else {
                        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    }
                }
            )
        }
    }
}
