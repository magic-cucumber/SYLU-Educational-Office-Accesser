package top.kagg886.eoa

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.eoa.theme.AppTheme

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/1/8 12:58
 * ================================================
 */


@Composable
fun ImageProcessingApp(todo: ImageBitmap) = AppTheme(
    color = AppSettingsMMKV.color,
    nightTheme = isSystemInDarkTheme(),
) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("${todo.width}x${todo.height}")
        }
    }
}
