package top.kagg886.eoa.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Stable
class GuideScaffoldState internal constructor() {
    private var progressState by mutableFloatStateOf(0f)
    private var subtitleHeightPx by mutableIntStateOf(0)
    private var titleHeightPx by mutableIntStateOf(0)

    var expandedTitleHeightPx by mutableIntStateOf(0)
        internal set

    var progress: Float
        get() = progressState
        set(value) {
            progressState = value.coerceIn(0f, 1f)
        }

    internal fun updateHeaderItemHeight(isSubtitle: Boolean, height: Int) {
        if (isSubtitle) {
            subtitleHeightPx = height
        } else {
            titleHeightPx = height
        }

        val headerHeight = subtitleHeightPx + titleHeightPx
        if (headerHeight > 0 && (progress == 0f || expandedTitleHeightPx == 0)) {
            expandedTitleHeightPx = headerHeight
        }
    }
}

@Composable
fun rememberGuideScaffoldState(): GuideScaffoldState = remember { GuideScaffoldState() }

// thanks to StageGuard(from Animeko)
// link: https://www.figma.com/design/LET1n9mmDa6npDTIlUuJjU/Animeko?node-id=349-9250&t=hBPSAEVlsmuEWPJt-0
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScaffold(
    modifier: Modifier = Modifier,
    state: GuideScaffoldState = rememberGuideScaffoldState(),
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    skipButton: (@Composable () -> Unit)? = null,
    backButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            GuideHeader(
                state = state,
                title = title,
                subTitle = subTitle,
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider()
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        Modifier.padding(start = 32.dp),
                    ) {
                        backButton?.invoke()
                    }

                    Row(
                        Modifier.weight(1f).padding(end = 32.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            Modifier.padding(horizontal = 8.dp),
                        ) {
                            skipButton?.invoke()
                        }
                        confirmButton.invoke()
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun GuideHeader(
    state: GuideScaffoldState,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
) {
    val progress = state.progress
    val expandedSubtitleStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.primary,
    )
    val expandedTitleStyle = MaterialTheme.typography.headlineMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 36.sp,
    )
    val collapsedTitleStyle = expandedSubtitleStyle.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )

    Layout(
        content = {
            Box(
                Modifier
                    .graphicsLayer {
                        alpha = 1f - progress
                    }
                    .onSizeChanged {
                        state.updateHeaderItemHeight(isSubtitle = true, height = it.height)
                    },
            ) {
                CompositionLocalProvider(LocalTextStyle provides expandedSubtitleStyle) {
                    subTitle()
                }
            }
            Box(
                Modifier.onSizeChanged {
                    state.updateHeaderItemHeight(isSubtitle = false, height = it.height)
                },
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides lerp(
                        expandedTitleStyle,
                        collapsedTitleStyle,
                        progress,
                    ),
                ) {
                    title()
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val horizontalPadding = 16.dp.roundToPx()
        val bottomPadding = 16.dp.roundToPx()
        val appBarHeight = TopAppBarDefaults.MediumAppBarCollapsedHeight.roundToPx()
        val titleConstraints = constraints.copy(
            minWidth = 0,
            maxWidth = (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0),
            minHeight = 0,
        )
        val subtitlePlaceable = measurables[0].measure(titleConstraints)
        val titlePlaceable = measurables[1].measure(titleConstraints)
        val expandedTitleHeight = state.expandedTitleHeightPx.takeIf { it > 0 }
            ?: (subtitlePlaceable.height + titlePlaceable.height)
        val expandedHeight = appBarHeight + expandedTitleHeight + bottomPadding
        val collapsedHeight = appBarHeight + titlePlaceable.height + bottomPadding
        val currentHeight = interpolate(expandedHeight, collapsedHeight, progress)
        val expandedSubtitleY = appBarHeight
        val expandedTitleY = expandedSubtitleY + subtitlePlaceable.height
        val collapsedTitleY = expandedSubtitleY
        val currentTitleY = interpolate(expandedTitleY, collapsedTitleY, progress)

        layout(constraints.maxWidth, currentHeight) {
            subtitlePlaceable.placeRelative(horizontalPadding, expandedSubtitleY)
            titlePlaceable.placeRelative(horizontalPadding, currentTitleY)
        }
    }
}

private fun interpolate(start: Int, end: Int, fraction: Float): Int {
    return (start + (end - start) * fraction).roundToInt()
}
