package top.kagg886.eoa.widget.ui

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import top.kagg886.eoa.AppActivity
import top.kagg886.eoa.androidApp.R
import top.kagg886.eoa.widget.LocalInnerRadius
import top.kagg886.eoa.widget.component.RefreshButton
import top.kagg886.eoa.widget.repository.TodayClass
import top.kagg886.eoa.widget.util.WidgetUtils
import top.kagg886.util.toFixed
import kotlin.random.Random
import kotlin.time.Clock

private val CourseSpacing = 8.dp
private val HeaderFontSize = 16.sp
private val NameFontSize = 12.sp
private val DetailFontSize = 10.sp
private val NameTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
private val CourseTimeFormat = LocalTime.Format {
    hour()
    chars(":")
    minute()
}

@SuppressLint("RestrictedApi")
@Composable
fun TodayCourseContent(
    courses: Result<List<TodayClass>>?,
    modifier: GlanceModifier = GlanceModifier
) {
    val widgetSize = LocalSize.current
    val outerPadding = LocalInnerRadius.current / 2
    val headerHeight = maxOf(32.dp, measureTextHeight(HeaderFontSize, Typeface.DEFAULT_BOLD, true))
    val detailHeight = measureTextHeight(DetailFontSize, Typeface.DEFAULT)
    val layout = CourseLayout(
        size = DpSize(
            width = (widgetSize.width - outerPadding * 2).coerceAtLeast(0.dp),
            height = (widgetSize.height - outerPadding * 2 - headerHeight - CourseSpacing).coerceAtLeast(0.dp)
        ),
        nameHeight = maxOf(measureTextHeight(NameFontSize, NameTypeface), detailHeight),
        detailHeight = detailHeight
    )

    Box(modifier = modifier.clickable(actionStartActivity<AppActivity>())) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(headerHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.width(CourseSpacing))
                Text(
                    text = "今日课程",
                    style = TextStyle(
                        fontSize = HeaderFontSize,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(MaterialTheme.colorScheme.onSurface)
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                RefreshButton(
                    onClick = WidgetUtils.createRefreshWidgetAction(),
                    tint = ColorProvider(MaterialTheme.colorScheme.onSurface)
                )
            }
            Spacer(modifier = GlanceModifier.height(CourseSpacing))
            Box(modifier = GlanceModifier.fillMaxWidth().height(layout.size.height)) {
                when {
                    courses == null -> EmptyCoursesView("Loading...")
                    courses.isFailure -> EmptyCoursesView(courses.exceptionOrNull()?.message ?: "课程加载失败")
                    else -> CoursesList(courses.getOrThrow(), layout)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun EmptyCoursesView(message: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(MaterialTheme.colorScheme.onSurface)
            )
        )
    }
}

@Composable
private fun CoursesList(courses: List<TodayClass>, layout: CourseLayout) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val upcomingCourses = courses.filter { it.date.second > now }
    if (upcomingCourses.isEmpty()) {
        EmptyCoursesView(if (courses.isEmpty()) "今日无课程!" else "今日课程已结束")
        return
    }
    if (layout.capacity == 0 || layout.size.width <= 0.dp) {
        EmptyCoursesView("空间不足，点击查看课程")
        return
    }

    val visibleCourses = upcomingCourses.take(layout.capacity)
    val radius = LocalInnerRadius.current
    val gap = layout.gapFor(visibleCourses.size)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        visibleCourses.forEachIndexed { index, course ->
            val topRadius = if (index == 0) radius else 0.dp
            val bottomRadius = if (index == visibleCourses.lastIndex) radius else 0.dp
            val bottomGap = if (index == visibleCourses.lastIndex) 0.dp else gap
            val action = if (course.conflict) {
                WidgetUtils.createCourseConflictAction(course.date.first, course.date.second)
            } else {
                WidgetUtils.createCourseDetailAction(course.recordId)
            }

            // One Column child per course; the extra height leaves room for the gap.
            Box(modifier = GlanceModifier.fillMaxWidth().height(layout.itemHeight + bottomGap)) {
                Card(
                    size = DpSize(layout.size.width, layout.itemHeight),
                    modifier = GlanceModifier.clickable(action),
                    corner = RoundedCornerShape(
                        topStart = topRadius,
                        topEnd = topRadius,
                        bottomStart = bottomRadius,
                        bottomEnd = bottomRadius
                    ),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    CourseItem(course, layout)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun CourseItem(course: TodayClass, layout: CourseLayout) {
    val color = MaterialTheme.colorScheme.onSurface
    val accentColor = remember(course.name) {
        Color.hsv(
            hue = Random(course.name.hashCode()).nextInt(36000) / 100f,
            saturation = 0.6412f,
            value = 1f
        )
    }
    val time = course.date.first.time.format(CourseTimeFormat)
    val detail = if (course.conflict) time else "$time • ${course.location}"
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(CourseSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            size = DpSize(4.dp, layout.nameHeight + layout.detailHeight),
            corner = RoundedCornerShape(2.dp),
            backgroundColor = accentColor
        ) {}
        Spacer(modifier = GlanceModifier.width(CourseSpacing))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(layout.nameHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CourseText(
                    text = course.name,
                    fontSize = NameFontSize,
                    color = color,
                    modifier = GlanceModifier.defaultWeight(),
                    medium = true
                )
                course.progress?.let { progress ->
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    // Keep Glance Text here so RemoteViews cannot consume the course name's width.
                    Text(
                        text = "${(progress * 100).toFixed(2)}%",
                        style = TextStyle(fontSize = DetailFontSize, color = ColorProvider(color)),
                        maxLines = 1
                    )
                }
            }
            CourseText(
                text = detail,
                fontSize = DetailFontSize,
                color = color,
                modifier = GlanceModifier.fillMaxWidth().height(layout.detailHeight)
            )
        }
    }
}

@Composable
private fun CourseText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: GlanceModifier = GlanceModifier,
    medium: Boolean = false
) {
    require(fontSize.isSp) { "fontSize must use sp." }
    val packageName = LocalContext.current.packageName
    val remoteViews = remember(packageName, text, fontSize, color, medium) {
        val layout = if (medium) R.layout.widget_course_name_text else R.layout.widget_course_detail_text
        val id = if (medium) R.id.widget_course_name_text else R.id.widget_course_detail_text
        RemoteViews(packageName, layout).apply {
            setTextViewText(id, text)
            setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, fontSize.value)
            setTextColor(id, color.toArgb())
        }
    }
    AndroidRemoteViews(remoteViews = remoteViews, modifier = modifier)
}

private data class CourseLayout(
    val size: DpSize,
    val nameHeight: Dp,
    val detailHeight: Dp
) {
    val itemHeight: Dp = nameHeight + detailHeight + CourseSpacing * 2
    val capacity: Int = (size.height / itemHeight).toInt().coerceIn(0, 10)

    fun gapFor(itemCount: Int): Dp {
        if (itemCount <= 1) return 0.dp
        val remainingHeight = (size.height - itemHeight * itemCount).coerceAtLeast(0.dp)
        val availableGap = remainingHeight / (itemCount - 1)
        return if (itemCount < capacity) minOf(CourseSpacing, availableGap) else availableGap
    }
}

@Composable
private fun measureTextHeight(
    fontSize: TextUnit,
    typeface: Typeface,
    includePadding: Boolean = false
): Dp {
    val density = LocalDensity.current
    return remember(density, fontSize, typeface, includePadding) {
        // Paint is a pixel API; expose only the measured layout height in dp.
        val metrics = Paint().apply {
            this.typeface = typeface
            textSize = with(density) { fontSize.toPx() }
        }.fontMetricsInt
        val heightPx = if (includePadding) metrics.bottom - metrics.top else metrics.descent - metrics.ascent
        with(density) { heightPx.coerceAtLeast(1).toDp() }
    }
}
