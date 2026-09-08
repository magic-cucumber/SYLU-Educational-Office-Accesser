package top.kagg886.eoa.component.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus

/** Header and CourseLayout must receive the same weekdays and timeline width. */
@Composable
fun CourseComponentHeader(
    weekStartDate: LocalDate,
    currentDate: LocalDate,
    modifier: Modifier = Modifier,
    allowIsoWeekNumber: List<Int> = ComponentDefault.AllowIsoWeekNumber,
    timelineWidth: Dp = ComponentDefault.TimelineWidth,
) {
    validateDays(allowIsoWeekNumber)
    Row(modifier.height(ComponentDefault.HeaderHeight).fillMaxWidth()) {
        MonthHeader(weekStartDate.month.number, Modifier.width(timelineWidth).fillMaxHeight())
        for (day in allowIsoWeekNumber) {
            val date = weekStartDate.plus(day - 1, DateTimeUnit.DAY)
            DayHeader(date, day, date == currentDate, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun MonthHeader(
    month: Int, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        Surface(modifier = Modifier.height(with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 4.dp + 28.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.5f
            ),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(10.dp)) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${month}月",
                    modifier = Modifier.padding(
                        horizontal = 8.dp, vertical = 4.dp
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate, dayOfWeek: Int, isCurrentDay: Boolean, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfWeekName(dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrentDay) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(
                if (isCurrentDay) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentDay) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = if (isCurrentDay) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
        }
    }
}

private fun dayOfWeekName(
    dayOfWeek: Int
): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> error(
        "Invalid day of week: $dayOfWeek"
    )
}
