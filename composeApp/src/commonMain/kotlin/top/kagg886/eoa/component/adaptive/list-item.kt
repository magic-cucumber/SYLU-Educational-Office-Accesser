package top.kagg886.eoa.component.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.kagg886.eoa.util.currentLayoutType

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 16:03
 * ================================================
 */

@DslMarker
internal annotation class AdaptiveListItemDsl

@Composable
fun AdaptiveListItem(
    layout: NavigationSuiteType = currentLayoutType(),
    modifier: Modifier = Modifier,
    headlineContent: @Composable () -> Unit,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    actions: AdaptiveListItemScope.() -> Unit = {},
) {
    val actionsState by rememberAdaptiveListItemActions(actions)

    when (layout) {
        NavigationSuiteType.NavigationBar -> {

            val swipeState = rememberSwipeToDismissBoxState()

            LaunchedEffect(swipeState.currentValue) {
                when (swipeState.currentValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        actionsState.primaryAction?.takeIf { action -> action.canUseAsAction }?.clickable?.invoke()
                        swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        actionsState.secondAction?.takeIf { action -> action.canUseAsAction }?.clickable?.invoke()
                        swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                    }

                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }

            SwipeToDismissBox(
                state = swipeState,
                modifier = modifier,
                enableDismissFromStartToEnd = actionsState.primaryAction.canUseAsAction,
                enableDismissFromEndToStart = actionsState.secondAction.canUseAsAction,
                backgroundContent = {
                    val action = when (swipeState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> actionsState.primaryAction
                        SwipeToDismissBoxValue.EndToStart -> actionsState.secondAction
                        SwipeToDismissBoxValue.Settled -> null
                    }
                    val color = when (swipeState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        SwipeToDismissBoxValue.Settled -> Color.Unspecified
                    }

                    Surface(color = color, modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                Alignment.CenterEnd
                            } else {
                                Alignment.CenterStart
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(16.dp).size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                action?.icon?.invoke()
                            }
                        }
                    }
                },
                content = {
                    ListItem(
                        headlineContent = headlineContent,
                        overlineContent = overlineContent,
                        supportingContent = supportingContent,
                    )
                },
            )
        }

        else -> {
            ListItem(
                headlineContent = headlineContent,
                overlineContent = overlineContent,
                supportingContent = supportingContent,
                modifier = modifier,
                trailingContent = {
                    Row {
                        actionsState.primaryAction?.let { action ->
                            IconButton(
                                onClick = action.clickable,
                                enabled = action.canUseAsAction,
                            ) {
                                action.icon?.invoke()
                            }
                        }

                        actionsState.secondAction?.let { action ->
                            IconButton(
                                onClick = action.clickable,
                                enabled = action.canUseAsAction,
                            ) {
                                action.icon?.invoke()
                            }
                        }
                    }
                }
            )
        }
    }
}

@AdaptiveListItemDsl
class AdaptiveListItemScope internal constructor() {
    private var primaryAction: AdaptiveListItemAction? = null
    private var secondAction: AdaptiveListItemAction? = null

    fun primaryAction(content: AdaptiveListItemActionScope.() -> Unit) {
        primaryAction = AdaptiveListItemActionScope().apply(content).toAction()
    }

    fun secondAction(content: AdaptiveListItemActionScope.() -> Unit) {
        secondAction = AdaptiveListItemActionScope().apply(content).toAction()
    }

    internal fun toState(): AdaptiveListItemActions = AdaptiveListItemActions(
        primaryAction = primaryAction,
        secondAction = secondAction,
    )
}

@AdaptiveListItemDsl
class AdaptiveListItemActionScope internal constructor() {
    var enable: Boolean = true
    private var icon: (@Composable () -> Unit)? = null
    private var clickable: () -> Unit = {}

    fun icon(content: @Composable () -> Unit) {
        icon = content
    }

    fun clickable(block: () -> Unit) {
        clickable = block
    }

    internal fun toAction(): AdaptiveListItemAction = AdaptiveListItemAction(
        icon = icon,
        enabled = enable,
        clickable = clickable,
    )
}

internal data class AdaptiveListItemActions(
    val primaryAction: AdaptiveListItemAction?,
    val secondAction: AdaptiveListItemAction?,
)

internal data class AdaptiveListItemAction(
    val icon: (@Composable () -> Unit)?,
    val enabled: Boolean,
    val clickable: () -> Unit,
)

private val AdaptiveListItemAction?.canUseAsAction: Boolean
    get() = this?.enabled == true && icon != null

@Composable
private fun rememberAdaptiveListItemActions(
    content: AdaptiveListItemScope.() -> Unit
): State<AdaptiveListItemActions> {
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf { AdaptiveListItemScope().apply(latestContent.value).toState() }
    }
}
