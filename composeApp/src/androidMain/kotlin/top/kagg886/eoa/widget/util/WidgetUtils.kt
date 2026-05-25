package top.kagg886.eoa.widget.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import top.kagg886.eoa.AppActivity
import top.kagg886.eoa.EOAApplication
import top.kagg886.eoa.widget.TodayCourseWidget
import kotlin.random.Random
import kotlin.time.Clock

/**
 * 小组件工具类
 */
object WidgetUtils {

    /**
     * 获取课程颜色
     */
    fun getCourseColor(courseName: String, isDarkMode: Boolean): Color {
        return if (!isDarkMode) {
            Color.hsv(
                hue = Random(courseName.hashCode()).nextInt(36000) / 100.0f,
                saturation = 0.1412f,
                value = 1f
            )
        } else {
            Color.hsv(
                hue = Random(courseName.hashCode()).nextInt(36000) / 100.0f,
                saturation = 0.3038f,
                value = 0.3039f
            )
        }
    }

    /**
     * 获取星期几的中文名称
     */
    fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }

    /**
     * 格式化时间段
     */
    fun formatPeriod(period: Int): String {
        return "#$period"
    }


    /**
     * 创建刷新小组件的Action
     */
    fun createRefreshWidgetAction() = actionRunCallback<RefreshWidgetCallback>()

    /**
     * 创建课程详情深层链接的Action
     */
    fun createCourseDetailAction(recordId: Long) = actionStartActivity(
        Intent(Intent.ACTION_VIEW, "eoa://course/profile/$recordId".toUri()).apply {
            setClass(EOAApplication.getApp(), AppActivity::class.java)
        }
    )

    fun createCourseConflictAction(weekNumber: Int, periodOfDay: Int) = actionStartActivity(
        Intent(
            Intent.ACTION_VIEW,
            "eoa://course/conflict/$weekNumber/${
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.isoDayNumber
            }/$periodOfDay".toUri()
        ).apply {
            setClass(EOAApplication.getApp(), AppActivity::class.java)
        }
    )
}


class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        TodayCourseWidget().update(context, glanceId)
    }
}
