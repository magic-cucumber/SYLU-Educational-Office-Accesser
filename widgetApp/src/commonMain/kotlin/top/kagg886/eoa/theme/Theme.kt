package top.kagg886.eoa.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme

@Composable
fun AppTheme(
    color: Color,
    nightTheme: Boolean,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    SystemAppearance(systemIsDark)

    AnimatedContent(nightTheme, transitionSpec = { fadeIn(initialAlpha = 0.5f) togetherWith fadeOut(targetAlpha = 0.5f) }) { isDark ->
        DynamicMaterialTheme(
            seedColor = color,
            isDark = isDark,
            content = content,
        )
    }
}

@Composable
expect fun SystemAppearance(isDark: Boolean)
