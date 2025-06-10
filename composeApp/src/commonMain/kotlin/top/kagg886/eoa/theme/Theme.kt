package top.kagg886.eoa.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

@Composable
internal fun AppTheme(
    color: Color,
    nightTheme: Boolean,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()

    SystemAppearance(systemIsDark)
    DynamicMaterialTheme(
        seedColor = color,
        useDarkTheme = nightTheme,
        style = PaletteStyle.Fidelity,
        content = content
    )
}

@Composable
internal expect fun SystemAppearance(isDark: Boolean)
