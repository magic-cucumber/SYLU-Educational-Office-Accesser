package top.kagg886.report

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.backend.database.AppDatabase
import kotlin.random.Random

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/11 16:09
 * ================================================
 */

@Composable
fun CrashApp(
    database: AppDatabase,
    error: String,
    onRestart: () -> Unit,
) {
    val random = remember {
        Random(error.hashCode()).nextInt(36000) / 100.0f
    }
    val color = when((AppSettingsMMKV.theme == AppSettingsMMKVType.AppTheme.SystemDefault && isSystemInDarkTheme()) || AppSettingsMMKV.theme == AppSettingsMMKVType.AppTheme.Dark) {
        true -> Color.hsv(
            hue = random,
            saturation = 0.1412f,
            value = 1f
        )
        false -> Color.hsv(
            hue = random,
            saturation = 0.3038f,
            value = 0.3039f
        )
    }
    AppTheme(color = color, nightTheme = isSystemInDarkTheme()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("程序崩溃了...")
        }
    }
}