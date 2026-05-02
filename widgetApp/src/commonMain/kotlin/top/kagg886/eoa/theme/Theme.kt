package top.kagg886.eoa.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme

@Composable
internal fun AppTheme(
    color: Color,
    nightTheme: Boolean,
    content: @Composable () -> Unit
) {
    SystemAppearance(nightTheme)
    DynamicMaterialTheme(
        seedColor = color,
        isDark = nightTheme,
        content = content,
    )
}

@Composable
internal expect fun SystemAppearance(isDark: Boolean)
