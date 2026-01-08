package top.kagg886.eoa

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
                    .padding(16.dp), // 留出 10% 边框
                contentAlignment = Alignment.Center
            ) {
                val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

                Surface(modifier = Modifier.wrapContentSize(), shape = RoundedCornerShape(bottomInset)) {
                    Image(
                        bitmap = todo,
                        contentDescription = "Cover Image",
                        // Fit 模式：保证不拉伸，至少一轴填满容器，且不裁剪
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.wrapContentSize()
                    )

//                    // 文字现在是相对于“图片显示的实际大小”进行对齐
//                    Text(
//                        text = "文字仅在图片显示范围内",
//                        style = MaterialTheme.typography.labelSmall,
//                        modifier = Modifier
//                            .align(Alignment.TopStart) // 紧贴图片左上角
//                            .background(Color.Black.copy(alpha = 0.6f))
//                            .padding(4.dp)
//                    )
                }
            }

            // --- 下半部分 (2/10) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {

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
