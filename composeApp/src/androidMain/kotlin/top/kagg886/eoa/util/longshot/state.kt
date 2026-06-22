package top.kagg886.eoa.util.longshot

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:43
 * ================================================
 */



internal val LocalLongShotTargetRegistry = staticCompositionLocalOf<LongShotTargetRegistry> { error("not provided") }

class LongShotTargetRegistry {
    private val activeTarget = MutableStateFlow<LongShotTarget?>(null)

    fun register(target: LongShotTarget) {
        val current = activeTarget.value
        if (current !== target) {
            logger.i("注册长截屏目标：${target::class.simpleName}")
            activeTarget.value = target
            return
        }
        logger.w("长截屏目标已注册：${target::class.simpleName}")
    }

    fun unregister(target: LongShotTarget) {
        if (activeTarget.value === target) {
            logger.d("卸载长截屏目标：${target::class.simpleName}")
            activeTarget.value = null
        }
    }

    fun target(): LongShotTarget? = activeTarget.value
}

interface LongShotTarget {
    fun canScrollVertically(direction: Int): Boolean
    fun scrollBy(x: Int, y: Int)
}

internal class ScrollableStateLongShotTarget(
    private val state: ScrollableState,
    private val scope: CoroutineScope
) : LongShotTarget {
    override fun canScrollVertically(direction: Int): Boolean {
        val result = if (direction > 0) {
            state.canScrollForward
        } else {
            state.canScrollBackward
        }
        logger.d("ScrollState target canScrollVertically(direction=$direction) -> $result")
        return result
    }

    override fun scrollBy(x: Int, y: Int) {
        logger.d("ScrollState target scrollBy(x=$x, y=$y)")
        scope.launch {
            state.scrollBy(y.toFloat())
            logger.d("ScrollState target scrollBy 完成")
        }
    }
}
