package top.kagg886.eoa.util

import androidx.compose.animation.*

fun <S> createMenuButtonAnim(block: AnimatedContentTransitionScope<S>.() -> Boolean): AnimatedContentTransitionScope<S>.() -> ContentTransform =
    {
        if (block()) {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
        } else {
            slideInVertically { height -> -height } + fadeIn() togetherWith
                    slideOutVertically { height -> height } + fadeOut()
        }
    }
