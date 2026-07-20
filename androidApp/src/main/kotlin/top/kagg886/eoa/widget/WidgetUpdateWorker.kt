package top.kagg886.eoa.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import top.kagg886.util.asTaggedLogger
import java.util.concurrent.TimeUnit

/**
 * 小组件更新工作器
 * 定期更新小组件数据
 */
class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val logger = "WidgetUpdateWorker".asTaggedLogger

    override suspend fun doWork(): Result {
        return try {
            logger.d("小组件定时更新")
            // 更新今日课程小组件
            TodayCourseWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateWorker", "Failed to update widgets", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_update_work"

        /**
         * 启动定期更新任务
         */
        fun startPeriodicUpdate(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.MINUTES // 每30分钟更新一次
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * 停止定期更新任务
         */
        fun stopPeriodicUpdate(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
