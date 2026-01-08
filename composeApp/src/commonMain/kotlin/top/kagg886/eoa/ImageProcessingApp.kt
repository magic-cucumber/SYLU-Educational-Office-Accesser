package top.kagg886.eoa

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.saveImageToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.eoa.theme.AppTheme

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/1/8 12:58
 * ================================================
 */


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageProcessingApp(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    todo: ImageBitmap,
    exit: () -> Unit,
) = AppTheme(
    color = AppSettingsMMKV.color,
    nightTheme = isSystemInDarkTheme(),
) {
    Surface(modifier, color = background) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text("效果预览")
                },
                subtitle = {
                    Text("若要修改效果，请前往 SYLU-EOA 系统设置")
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = background
                )
            )

            val scope = rememberCoroutineScope { Dispatchers.IO }
            val graphicsLayer = rememberGraphicsLayer()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .padding(16.dp)
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    },
                contentAlignment = Alignment.Center
            ) {
                val bottomInset = with(WindowInsets.safeDrawing.asPaddingValues()) {
                    (calculateTopPadding() + calculateLeftPadding(LocalLayoutDirection.current)) / 2
                }

                Surface(modifier = Modifier.wrapContentSize()) {
                    Image(
                        bitmap = todo,
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(bottomInset))
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.3f),
                        color = background,
                        shape = SpecialFolderShape(bottomInset)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                if (AppSyncMMKV.profile == null) {
                                    Text(
                                        text = "请先登录 SYLU-EOA 然后再进行预览。"
                                    )
                                    return@ProvideTextStyle
                                }
                                Text(AppLoginPropertiesMMKV.username)
                                Text(AppSyncMMKV.profile!!.name)
                            }
                        }
                    }

                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.1f)
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            FileKit.saveImageToGallery(
                                graphicsLayer.toImageBitmap().toByteArray(),
                                filename = "cover.png"
                            )
                            exit()
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, "Save")
                }
                IconButton(
                    onClick = {

                    }
                ) {
                    Icon(Icons.Default.Share, "Share")
                }
            }
        }
    }
}

internal expect suspend fun ImageBitmap.toByteArray(): ByteArray


private class SpecialFolderShape(private val cornerSize: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = with(density) { cornerSize.toPx() }
        val path = Path().apply {
            reset()
            // 1. 从左上角圆角开始 (顺时针绘制)
            moveTo(0f, radius)
            arcTo(
                rect = Rect(0f, 0f, radius * 2, radius * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 2. 绘制顶部直线到右上角
            lineTo(size.width - radius, 0f)

            // 3. 右上角圆角
            arcTo(
                rect = Rect(size.width - radius * 2, 0f, size.width, radius * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 4. 右侧直线向下，直到下半部分开始“外拐”的位置
            lineTo(size.width, size.height - radius)

            // 5. 右下角：向外拐的圆角 (关键点)
            // 这里我们使用 arcTo，但注意矩形的中心点在外面
            arcTo(
                rect = Rect(size.width, size.height - radius * 2, size.width + radius * 2, size.height),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f, // 负数代表反向旋转，形成内凹/外拐效果
                forceMoveTo = false
            )

            // 6. 底部直线向左
            lineTo(-radius, size.height)

            // 7. 左下角：向外拐的圆角
            arcTo(
                rect = Rect(-radius * 2, size.height - radius * 2, 0f, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )

            close()
        }
        return Outline.Generic(path)
    }
}
