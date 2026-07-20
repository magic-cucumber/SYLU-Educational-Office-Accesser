package top.kagg886.eoa.widget.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider

/**
 * 小组件图标按钮组件
 */
@Composable
fun WidgetIconButton(
    imageProvider: ImageProvider,
    contentDescription: String,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    backgroundColor: Color = Color.Transparent,
    tint: ColorProvider? = null
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(backgroundColor)
            .clickable(onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = imageProvider,
            contentDescription = contentDescription,
            colorFilter = tint?.let { androidx.glance.ColorFilter.tint(it) }
        )
    }
}

/**
 * 刷新按钮
 */
@Composable
fun RefreshButton(
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    tint: ColorProvider? = null
) {
    WidgetIconButton(
        imageProvider = ImageProvider(android.R.drawable.ic_popup_sync),
        contentDescription = "刷新",
        onClick = onClick,
        modifier = modifier,
        tint = tint
    )
}
