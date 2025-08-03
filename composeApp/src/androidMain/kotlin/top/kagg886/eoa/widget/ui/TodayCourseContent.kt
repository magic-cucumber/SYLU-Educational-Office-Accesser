package top.kagg886.eoa.widget.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import top.kagg886.eoa.AppActivity
import top.kagg886.eoa.widget.component.RefreshButton
import top.kagg886.eoa.widget.repository.TodayClass
import top.kagg886.eoa.widget.util.WidgetUtils
import top.kagg886.util.toFixed
import kotlin.random.Random

/**
 * 今日课程小组件内容
 */
@SuppressLint("RestrictedApi")
@Composable
fun TodayCourseContent(
    courses: Result<List<TodayClass>>?,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .clickable(actionStartActivity<AppActivity>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // 标题栏
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = "今日课程",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(MaterialTheme.colorScheme.onSurface)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // 刷新按钮
                RefreshButton(
                    onClick = WidgetUtils.createRefreshWidgetAction(),
                    tint = ColorProvider(MaterialTheme.colorScheme.onSurface)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))


            when {
                courses == null -> EmptyCoursesView("Loading...")
                courses.isFailure -> EmptyCoursesView(courses.exceptionOrNull()!!.message!!)
                else -> CoursesList(courses.getOrThrow())
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
private fun CoursesList(courses: List<TodayClass>) {
    LazyColumn {
        itemsIndexed(courses) { index, course ->
            Column {
                if (index != 0) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
                CourseItem(
                    modifier = GlanceModifier.padding(8.dp).cornerRadius(8.dp),
                    course = course
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun CourseItem(modifier: GlanceModifier = GlanceModifier, course: TodayClass) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(WidgetUtils.createCourseDetailAction(course.recordId))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(modifier = GlanceModifier.fillMaxHeight().width(4.dp)) {
                Spacer(modifier = GlanceModifier.width(4.dp))

                val basicColor = remember(course) {
                    Color.hsv(
                        hue = Random(course.name.hashCode()).nextInt(36000) / 100.0f,
                        saturation = 0.6412f,
                        value = 1f
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(basicColor)
                        .cornerRadius(4.dp)
                ) {}
                Spacer(modifier = GlanceModifier.width(4.dp))
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = course.name,
                    maxLines = 2,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(MaterialTheme.colorScheme.onSurface)
                    )
                )

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "${WidgetUtils.formatPeriod(course.period)} • ${course.location}",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(MaterialTheme.colorScheme.onSurface)
                        )
                    )
                    course.progress?.let {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "${(it * 100).toFixed(2)}%",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = ColorProvider(MaterialTheme.colorScheme.onSurface)
                            )
                        )
                    }
                }
            }
        }
    }
}
