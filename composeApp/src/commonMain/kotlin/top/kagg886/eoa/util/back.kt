package top.kagg886.eoa.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/4 15:50
 * ================================================
 */
@Composable
@Deprecated("use variant PredictiveBackHandler can predictive back instead.")
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)


@Composable
fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBackStarted: (NavigationEvent) -> Unit = {},
    onBackProgressed: (NavigationEvent) -> Unit = {},
    onBackCancelled: () -> Unit = {},
    onBack: () -> Unit,
) {
    val state = rememberNavigationEventState(
        NavigationEventInfo.None
    )

    val currentOnBackStarted by rememberUpdatedState(onBackStarted)
    val currentOnBackProgressed by rememberUpdatedState(onBackProgressed)

    LaunchedEffect(state) {
        var started = false

        snapshotFlow { state.transitionState }
            .collect { transition ->
                when (transition) {
                    is NavigationEventTransitionState.InProgress -> {
                        if (transition.direction != NavigationEventTransitionState.TRANSITIONING_BACK) {
                            return@collect
                        }
                        if (started) {
                            currentOnBackProgressed(transition.latestEvent)
                        } else {
                            started = true
                            currentOnBackStarted(transition.latestEvent)
                        }
                    }

                    is NavigationEventTransitionState.Idle -> {
                        started = false
                    }
                }
            }
    }

    NavigationBackHandler(
        state = state,
        isBackEnabled = enabled,
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBack,
    )
}