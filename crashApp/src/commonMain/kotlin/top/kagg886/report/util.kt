package top.kagg886.report

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.Clipboard

internal val DefaultTransform: AnimatedContentTransitionScope<*>.() -> ContentTransform =
    { (fadeIn(initialAlpha = 0.5f) togetherWith fadeOut(targetAlpha = 0.5f)).using(SizeTransform(clip = false)) }

expect suspend fun Clipboard.setText(text: String)