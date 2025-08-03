package top.kagg886.eoa.widget.util

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/3 22:27
 * ================================================
 */

/**
 * 将像素值转换为 Dp 单位
 * @param px 像素值
 * @return Dp 单位的值
 */
fun Context.dpFrom(px: Int): Dp {
    val density = resources.displayMetrics.density
    return (px / density).dp
}
