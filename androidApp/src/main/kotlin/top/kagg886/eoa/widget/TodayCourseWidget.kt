package top.kagg886.eoa.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.*
import androidx.glance.color.isNightMode
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.materialkolor.DynamicMaterialTheme
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.backend.database.databasePath
import top.kagg886.eoa.util.registerKermitLoggerIfExists
import top.kagg886.eoa.widget.repository.TodayClass
import top.kagg886.eoa.widget.repository.WidgetRepository
import top.kagg886.eoa.widget.ui.TodayCourseContent
import top.kagg886.eoa.widget.ui.Card
import top.kagg886.mkmb.MMKV
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.dataPath
import top.kagg886.util.initializeMMKV


val LocalInnerRadius = staticCompositionLocalOf<Dp> {
    error("not provided")
}

/**
 * 今日课程小组件
 * 尺寸：2x2，不可拖拽
 */
class TodayCourseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    private val logger = "TodayCourseWidget".asTaggedLogger
    private val database by lazy {
        logger.i("build database, dataPath=$dataPath, databasePath=$databasePath")
        val db = databaseBuilder().build()
        registerKermitLoggerIfExists(db.appLogDao())
        db
    }

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (!MMKV.initialized) {
            initializeMMKV()
        }
        val repository = WidgetRepository(database)
        logger.i("小组件: $id 准备绘制")
        val density = Density(
            density = context.resources.displayMetrics.density,
            fontScale = context.resources.configuration.fontScale,
        )
        provideContent {
            DynamicMaterialTheme(
                seedColor = AppSettingsMMKV.color,
                isDark = with(AppSettingsMMKV.theme) {
                    (this == AppSettingsMMKVType.AppTheme.Dark) || (this == AppSettingsMMKVType.AppTheme.SystemDefault && context.isNightMode)
                }
            ) {
                val corner = with(density) {
                    when {
                        AppSettingsMMKV.systemWidgetRadius && Build.VERSION.SDK_INT >= 31 ->
                            context.resources.getDimensionPixelSize(android.R.dimen.system_app_widget_background_radius).toDp()
                        else -> 28.dp
                    }
                }

                val padding = with(density) {
                    when {
                        AppSettingsMMKV.systemWidgetRadius && Build.VERSION.SDK_INT >= 31 ->
                            context.resources.getDimensionPixelSize(android.R.dimen.system_app_widget_inner_radius).toDp()
                        else -> 20.dp
                    }
                }

                CompositionLocalProvider(
                    LocalInnerRadius provides padding,
                    LocalDensity provides density,
                ) {
                    Card(
                        size = LocalSize.current,
                        modifier = GlanceModifier.fillMaxSize().appWidgetBackground(),
                        corner = RoundedCornerShape(corner),
                        backgroundColor = MaterialTheme.colorScheme.background
                    ) {
                        TodayCourseWidgetContent(
                            repository = repository,
                            modifier = GlanceModifier.fillMaxSize().padding(padding / 2)
                        )
                    }
                }
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
    var courses by remember {
        mutableStateOf<Result<List<TodayClass>>?>(null)
    }

    LaunchedEffect(Unit) {
        courses = runCatching { repository.getTodayCourses() }
    }

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
