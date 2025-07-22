package top.kagg886.eoa.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.color.isNightMode
import androidx.glance.layout.fillMaxSize
import com.materialkolor.DynamicMaterialTheme
import kotlinx.coroutines.runBlocking
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.EOAApplication
import top.kagg886.eoa.util.registerKermitLoggerIfExists
import top.kagg886.eoa.widget.repository.WidgetRepository
import top.kagg886.eoa.widget.ui.TodayCourseContent
import top.kagg886.mkmb.MMKV
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.initializeMMKV

/**
 * 今日课程小组件
 * 尺寸：2x2，不可拖拽
 */
class TodayCourseWidget : GlanceAppWidget() {
    private val logger = "TodayCourseWidget".asTaggedLogger

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (!MMKV.initialized) {
            initializeMMKV()
        }
        val repository = WidgetRepository(EOAApplication.getApp())
        registerKermitLoggerIfExists(repository.logDao)
        logger.i("小组件: $id 准备绘制")
        provideContent {
            DynamicMaterialTheme(
                seedColor = AppSettingsMMKV.color,
                useDarkTheme = with(AppSettingsMMKV.theme) {
                    (this == AppSettingsMMKVType.AppTheme.Dark) || (this == AppSettingsMMKVType.AppTheme.SystemDefault && context.isNightMode)
                }
            ) {
                TodayCourseWidgetContent(context, repository)
            }
        }
    }
}

@Composable
private fun TodayCourseWidgetContent(
    context: Context,
    repository: WidgetRepository
) {
    // 直接加载数据，但添加loading状态的视觉效果
    val courses = runBlocking { repository.getTodayCourses() }

    TodayCourseContent(
        context = context,
        courses = courses,
        modifier = GlanceModifier.fillMaxSize()
    )
}


/**
 * 今日课程小组件接收器
 */
class TodayCourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCourseWidget()

    override fun onEnabled(context: Context) {
        WidgetManager.initialize(context)
    }

    override fun onDisabled(context: Context) {
        WidgetManager.cleanup(context)
    }
}
