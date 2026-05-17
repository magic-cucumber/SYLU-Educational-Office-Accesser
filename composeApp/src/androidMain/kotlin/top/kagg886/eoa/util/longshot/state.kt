package top.kagg886.eoa.util.longshot

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:43
 * ================================================
 */



internal val LocalLongShotTargetRegistry = staticCompositionLocalOf<LongShotTargetRegistry> { error("not provided") }

private val logger = "LongShot".asTaggedLogger

class LongShotTargetRegistry {
    private val activeTarget = MutableStateFlow<LongShotTarget?>(null)
    val targetFlow: StateFlow<LongShotTarget?> = activeTarget

    fun register(target: LongShotTarget) {
        val current = activeTarget.value
        if (current !== target) {
            logger.i("注册长截屏目标：${target::class.simpleName}，fillerHeight=${target.fillerHeightDp}px")
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
    val fillerHeightDp: Int

    fun canScrollVertically(direction: Int): Boolean
    fun scrollBy(x: Int, y: Int)
    fun getScrollY(): Int
}
