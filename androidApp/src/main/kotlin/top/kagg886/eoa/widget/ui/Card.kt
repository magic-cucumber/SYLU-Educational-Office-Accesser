package top.kagg886.eoa.widget.ui

import android.view.View
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.size

/**
 * A bitmap keeps independent corner radii working on Android 6-9.
 * [size] includes content padding. The background does not clip child views.
 */
@Composable
fun Card(
    size: DpSize,
    modifier: GlanceModifier = GlanceModifier,
    corner: RoundedCornerShape = RoundedCornerShape(0.dp),
    backgroundColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalContext.current.resources.configuration
    val direction = if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val bitmap = remember(size, corner, backgroundColor, density, direction) {
        // Bitmap creation and outline drawing are pixel APIs.
        val width = with(density) { size.width.roundToPx().coerceAtLeast(1) }
        val height = with(density) { size.height.roundToPx().coerceAtLeast(1) }
        val image = ImageBitmap(width, height)
        val outline = corner.createOutline(Size(width.toFloat(), height.toFloat()), direction, density)
        val paint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
        }
        Canvas(image).drawOutline(outline, paint)
        image.asAndroidBitmap()
    }
    Box(modifier = modifier.size(size.width, size.height).background(ImageProvider(bitmap))) {
        content()
    }
}
