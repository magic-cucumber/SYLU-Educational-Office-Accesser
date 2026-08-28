package top.kagg886.report

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.Clipboard

internal val DefaultTransform: AnimatedContentTransitionScope<*>.() -> ContentTransform =
    { slideInVertically { height -> height } + fadeIn() togetherWith
            slideOutVertically { height -> -height } + fadeOut() }

expect suspend fun Clipboard.setText(text: String)