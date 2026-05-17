package top.kagg886.eoa.util.longshot

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:44
 * ================================================
 */

private val logger = "LongShot".asTaggedLogger


internal fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

internal fun defaultLongShotFillerHeightDp(): Int {
    val metrics = android.content.res.Resources.getSystem().displayMetrics
    return ((metrics.heightPixels / metrics.density) * 1.5f).toInt()
}


internal class ScrollStateLongShotTarget(
    private val scrollState: ScrollState,
    private val scope: CoroutineScope,
    override val fillerHeightDp: Int
) : LongShotTarget {
    override fun canScrollVertically(direction: Int): Boolean {
        val result = if (direction > 0) {
            scrollState.value < scrollState.maxValue
        } else {
            scrollState.value > 0
        }
        logger.i("ScrollState target canScrollVertically(direction=$direction) -> $result，value=${scrollState.value}，max=${scrollState.maxValue}")
        return result
    }

    override fun scrollBy(x: Int, y: Int) {
        logger.i("ScrollState target scrollBy(x=$x, y=$y)，before=${scrollState.value}")
        scope.launch {
            scrollState.scrollBy(y.toFloat())
            logger.i("ScrollState target scrollBy 完成，after=${scrollState.value}")
        }
    }

    override fun getScrollY(): Int {
        logger.i("ScrollState target getScrollY -> ${scrollState.value}")
        return scrollState.value
    }
}
