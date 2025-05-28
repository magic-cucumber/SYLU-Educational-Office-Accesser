package top.kagg886.eoa.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()


    SystemAppearance(systemIsDark)
    DynamicMaterialTheme(
        seedColor = Color(0x20c997),
        useDarkTheme = systemIsDark,
        content = content
    )
}

@Composable
internal expect fun SystemAppearance(isDark: Boolean)
