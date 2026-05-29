package top.kagg886.eoa.util.longshot

import android.content.Context
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:44
 * ================================================
 */

internal val logger = "LongShot".asTaggedLogger

internal fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

internal fun defaultLongShotFillerHeightDp(): Int {
    val metrics = android.content.res.Resources.getSystem().displayMetrics
    return ((metrics.heightPixels / metrics.density) * 1.5f).toInt()
}
