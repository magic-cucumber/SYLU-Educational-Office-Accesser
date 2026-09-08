package top.kagg886.eoa.component.course

import androidx.compose.ui.unit.dp

object ComponentDefault {
    val AllowIsoWeekNumber: List<Int> = listOf(1, 2, 3, 4, 5)
    val TimelineWidth = 48.dp
    val HeaderHeight = 64.dp
}

internal fun validateDays(days: List<Int>) {
    require(days.isNotEmpty() && days.distinct().size == days.size && days.all { it in 1..7 }) {
        "allowIsoWeekNumber must contain distinct ISO weekdays (1..7) and must not be empty."
    }
}
