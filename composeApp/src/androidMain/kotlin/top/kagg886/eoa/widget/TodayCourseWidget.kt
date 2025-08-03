package top.kagg886.eoa.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.isNightMode
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.materialkolor.DynamicMaterialTheme
import kotlinx.coroutines.runBlocking
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.eoa.EOAApplication
import top.kagg886.eoa.util.registerKermitLoggerIfExists
import top.kagg886.eoa.widget.repository.WidgetRepository
import top.kagg886.eoa.widget.ui.TodayCourseContent
import top.kagg886.eoa.widget.util.dpFrom
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
                val corner = with(context) {
                    when {
                        Build.VERSION.SDK_INT >= 31 -> dpFrom(resources.getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius))
                        else -> 28.dp
                    }
                }

                val padding = with(context) {
                    when {
                        Build.VERSION.SDK_INT >= 31 -> dpFrom(resources.getDimensionPixelSize(android.R.dimen.system_app_widget_inner_radius))
                        else -> 28.dp
                    }
                }
                TodayCourseWidgetContent(
                    repository = repository,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .cornerRadius(corner)
                        .padding(padding / 2)
                        .appWidgetBackground()
                )
            }
        }
    }
}

@Composable
private fun TodayCourseWidgetContent(
    modifier: GlanceModifier = GlanceModifier,
    repository: WidgetRepository
) {
    // 直接加载数据，但添加loading状态的视觉效果
    val courses = runBlocking { repository.getTodayCourses() }

    TodayCourseContent(
        courses = courses,
        modifier = modifier,
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
