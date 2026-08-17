package top.kagg886.eoa.pages.welcome.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kborowy.colorpicker.KolorPicker
import com.kborowy.colorpicker.config.PickerConfig
import com.kborowy.colorpicker.config.TrackConfig
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.GuideScaffold
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.pages.welcome.WelcomeScreen
import top.kagg886.eoa.pages.welcome.privacy.WelcomePrivacyRoute

@Serializable
data object WelcomeThemeRoute

@Composable
fun WelcomeThemeScreen() {
    val nav = LocalNavController.current
    val rootModel = rootViewModel()
    val rootState by rootModel.collectAsState()
    val theme by rootState.theme.collectAsState()
    val color by rootState.color.collectAsState()

    WelcomeScreen {
        GuideScaffold(
            subTitle = { Text("个性化你的使用体验") },
            title = { Text("选择应用主题") },
            backButton = { BackIconButton() },
            confirmButton = {
                Button(onClick = { nav.navigate(WelcomePrivacyRoute) }) {
                    Text("继续")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ColorSection(
                    color = color,
                    onColorSelected = rootModel::postNewColorSetting,
                )

                ThemeSection(
                    theme = theme,
                    onThemeSelected = rootModel::postNewThemeSetting,
                )

                Text(
                    text = "之后也可以在设置中随时修改。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ColorSection(
    color: Color,
    onColorSelected: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "主题色",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        var pickerDialog by remember { mutableStateOf(false) }
        if (pickerDialog) {
            AlertDialog(
                onDismissRequest = { pickerDialog = false },
                confirmButton = {
                    TextButton(onClick = { pickerDialog = false }) {
                        Text("确定")
                    }
                },
                title = { Text("自定义取色") },
                text = {
                    KolorPicker(
                        initialColor = color,
                        onColorSelected = onColorSelected,
                        pickerConfig = PickerConfig.Default,
                        alphaTrackConfig = TrackConfig.Default,
                        hueTrackConfig = TrackConfig.Default,
                        modifier = Modifier.size(260.dp),
                    )
                }
            )
        }

        val isCustom = color !in AppSettingsMMKV.presetsColor.values
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(AppSettingsMMKV.presetsColor.entries.toList()) { (_, builtInColor) ->
                ColorSwatch(
                    color = builtInColor,
                    selected = color == builtInColor,
                    onClick = { onColorSelected(builtInColor) },
                )
            }
            item(key = "custom") {
                ColorSwatch(
                    color = if (isCustom) color else null,
                    selected = isCustom,
                    onClick = { pickerDialog = true },
                )
            }
        }
    }
}

/**
 * 圆形色板。选中时在外侧绘制主题色描边圆环。
 * [color] 为 null 时表示"自定义"入口，用 HSV 生成柔和的彩虹渐变。
 */
@Composable
private fun ColorSwatch(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ringColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (color != null) {
                        Modifier.background(color)
                    } else {
                        Modifier.background(
                            Brush.sweepGradient(
                                List(7) { Color.hsv(hue = it * 60f, saturation = 0.55f, value = 0.9f) }
                            )
                        )
                    }
                )
                .clickable(onClick = onClick),
        )
        if (selected) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = ringColor,
                    radius = (size.minDimension / 2) - 1.dp.toPx(),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun ThemeSection(
    theme: AppSettingsMMKVType.AppTheme,
    onThemeSelected: (AppSettingsMMKVType.AppTheme) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "界面模式",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppSettingsMMKVType.AppTheme.entries.forEach { option ->
                ThemeCard(
                    option = option,
                    selected = option == theme,
                    onClick = { onThemeSelected(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    option: AppSettingsMMKVType.AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = when (option) {
                    AppSettingsMMKVType.AppTheme.Light -> Icons.Default.LightMode
                    AppSettingsMMKVType.AppTheme.SystemDefault -> Icons.Default.BrightnessAuto
                    AppSettingsMMKVType.AppTheme.Dark -> Icons.Default.DarkMode
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = when (option) {
                    AppSettingsMMKVType.AppTheme.Light -> "浅色"
                    AppSettingsMMKVType.AppTheme.SystemDefault -> "跟随系统"
                    AppSettingsMMKVType.AppTheme.Dark -> "深色"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

