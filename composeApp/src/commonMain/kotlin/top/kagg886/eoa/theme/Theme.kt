package top.kagg886.eoa.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme

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
        isDark = nightTheme,
        content = content,
    )
}
