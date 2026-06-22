package top.kagg886.eoa.util.longshot

import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/5/17 11:20
 * ================================================
 */

fun ComponentActivity.setContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit,
) {
    val existingLongShotRoot =
        window.decorView.findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as? LongShotRootView

    if (existingLongShotRoot != null) {
        logger.d("复用 MIUI 长截屏代理根布局")
        setOwners()
        existingLongShotRoot.setContent(parent, content)
    } else {
        logger.i("开始安装 MIUI 长截屏代理根布局")
        LongShotRootView(this).apply {
            // Set owners, parent and content before setContentView so attach-time code can see them.
            setOwners()
            setContent(parent, content)
            setContentView(this, DefaultActivityContentLayoutParams)
        }
        logger.i("MIUI 长截屏代理根布局安装完成")
    }
}

private val DefaultActivityContentLayoutParams =
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

/**
 * These owners are not set before AppCompat 1.3+ due to a bug, so we need to set them manually in
 * case developers are using an older version of AppCompat.
 */
private fun ComponentActivity.setOwners() {
    val decorView = window.decorView
    if (decorView.findViewTreeLifecycleOwner() == null) {
        decorView.setViewTreeLifecycleOwner(this)
    }
    if (decorView.findViewTreeViewModelStoreOwner() == null) {
        decorView.setViewTreeViewModelStoreOwner(this)
    }
    if (decorView.findViewTreeSavedStateRegistryOwner() == null) {
        decorView.setViewTreeSavedStateRegistryOwner(this)
    }
}

private class LongShotRootView(
    activity: ComponentActivity,
) : FrameLayout(activity) {
    private val registry = LongShotTargetRegistry()
    private val composeView = ComposeView(activity).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
    private val controlView = MiuiLongShotControlView(
        context = activity,
        registry = registry,
        contentView = composeView,
    ).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        addView(composeView)
        addView(controlView)
    }

    fun setContent(
        parent: CompositionContext?,
        content: @Composable () -> Unit,
    ) {
        composeView.setParentCompositionContext(parent)
        composeView.setContent {
            CompositionLocalProvider(LocalLongShotTargetRegistry provides registry) {
                content()
            }
        }
    }
}

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
private class MiuiLongShotControlView(
    context: Context,
    private val registry: LongShotTargetRegistry,
    private val contentView: View,
) : ScrollView(context) {
    private val fillerView = View(context)

    init {
        logger.d("创建 MIUI 长截屏代理 View")
        alpha = 0f
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER

        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                fillerView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Int.MAX_VALUE,
                ),
            )
        }
        addView(
            wrapper,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        logger.d("MIUI 长截屏代理 View 已挂载")
    }

    override fun onDetachedFromWindow() {
        logger.d("MIUI 长截屏代理 View 已卸载")
        super.onDetachedFromWindow()
    }

    override fun canScrollVertically(direction: Int): Boolean {
        val target = registry.target()
        val result = target?.canScrollVertically(direction) ?: false
        logger.d("代理 View canScrollVertically(direction=$direction) -> $result，target=${target?.let { it::class.simpleName } ?: "null"}")
        return result
    }

    override fun scrollBy(x: Int, y: Int) {
        val target = registry.target()
        logger.d("代理 View scrollBy(x=$x, y=$y)，target=${target?.let { it::class.simpleName } ?: "null"}")
        super.scrollBy(x, y)
        target?.scrollBy(x, y)
    }

    override fun setDrawingCacheEnabled(enabled: Boolean) {
        logger.d("代理 drawing cache enabled=$enabled")
        super.setDrawingCacheEnabled(enabled)
        contentView.isDrawingCacheEnabled = enabled
    }

    override fun isDrawingCacheEnabled(): Boolean {
        return contentView.isDrawingCacheEnabled
    }

    override fun buildDrawingCache(autoScale: Boolean) {
        logger.d("代理 buildDrawingCache(autoScale=$autoScale)")
        super.buildDrawingCache(autoScale)
        contentView.buildDrawingCache(autoScale)
    }

    override fun getDrawingCache(autoScale: Boolean): Bitmap? {
        logger.d("代理 getDrawingCache(autoScale=$autoScale)")
        return contentView.getDrawingCache(autoScale)
    }

    override fun destroyDrawingCache() {
        logger.d("代理 destroyDrawingCache")
        super.destroyDrawingCache()
        contentView.destroyDrawingCache()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false

    override fun onTouchEvent(ev: MotionEvent): Boolean = false
}
