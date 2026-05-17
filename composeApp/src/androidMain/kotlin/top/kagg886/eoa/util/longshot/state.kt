package top.kagg886.eoa.util.longshot

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:43
 * ================================================
 */



internal val LocalLongShotTargetRegistry = compositionLocalOf { LongShotTargetRegistry() }

private val logger = "LongShot".asTaggedLogger

class LongShotTargetRegistry {
    private val activeTarget = MutableStateFlow<LongShotTarget?>(null)
    val targetFlow: StateFlow<LongShotTarget?> = activeTarget

    fun register(target: LongShotTarget) {
        val current = activeTarget.value
        if (current != null && current !== target) {
            logger.i("注册长截屏目标失败：当前=${current::class.simpleName}，新目标=${target::class.simpleName}")
            throw IllegalStateException("Only one long shot target can be active in a page.")
        }
        logger.i("注册长截屏目标：${target::class.simpleName}，fillerHeight=${target.fillerHeightDp}px")
        activeTarget.value = target
    }

    fun unregister(target: LongShotTarget) {
        if (activeTarget.value === target) {
            logger.i("卸载长截屏目标：${target::class.simpleName}")
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
