package top.kagg886.eoa.widget

import android.content.Context
import top.kagg886.util.asTaggedLogger

/**
 * 小组件管理器
 * 提供小组件的初始化和更新功能
 */
object WidgetManager {
    private val logger = "WidgetManager".asTaggedLogger

    /**
     * 初始化小组件服务
     * 应在应用启动时调用
     */
    fun initialize(context: Context) {
        // 启动定期更新任务
        WidgetUpdateWorker.startPeriodicUpdate(context)
    }

    /**
     * 清理小组件服务
     * 应在应用关闭时调用
     */
    fun cleanup(context: Context) {
        WidgetUpdateWorker.stopPeriodicUpdate(context)
    }
}
