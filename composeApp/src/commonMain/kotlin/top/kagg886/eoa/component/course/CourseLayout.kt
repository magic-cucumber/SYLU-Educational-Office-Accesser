package top.kagg886.eoa.component.course

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.LocalTime
import kotlin.math.roundToInt

/**
 * Pure single-day time coordinates across the requested weekday columns.
 * Fills finite incoming constraints. Inside verticalScroll, supply Modifier.height(...).
 * Cards must be inside [startTime, endTime]; scrolling, clipping and gestures belong to the caller.
 */
@Composable
fun CourseLayout(
    startTime: LocalTime,
    endTime: LocalTime,
    modifier: Modifier = Modifier,
    allowIsoWeekNumber: List<Int> = ComponentDefault.AllowIsoWeekNumber,
    timelineWidth: Dp = ComponentDefault.TimelineWidth,
    content: @Composable CourseLayoutScope.() -> Unit,
) {
    require(startTime < endTime) { "CourseLayout requires startTime < endTime within one day." }
    validateDays(allowIsoWeekNumber)
    require(timelineWidth.value.isFinite() && timelineWidth.value >= 0) { "timelineWidth must be finite and nonnegative." }
    val scope = remember(startTime, endTime, allowIsoWeekNumber) {
        CourseLayoutScopeImpl(startTime, endTime, allowIsoWeekNumber.toList())
    }
    Layout(modifier = modifier, content = { scope.content() }) { measurables, constraints ->
        require(constraints.hasBoundedHeight) {
            "CourseLayout requires a finite height. Inside verticalScroll, provide Modifier.height(fullHeight)."
        }
        require(constraints.hasBoundedWidth) { "CourseLayout requires a finite width." }
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val axisWidth = timelineWidth.roundToPx()
        require(axisWidth <= width) { "timelineWidth must not exceed CourseLayout width." }
        require(measurables.count { it.parentData === TimelineData } <= 1) {
            "CourseLayout accepts at most one timeline()."
        }
        val duration = endTime.toNanosecondOfDay() - startTime.toNanosecondOfDay()
        fun y(time: LocalTime): Int =
            ((time.toNanosecondOfDay() - startTime.toNanosecondOfDay()).toDouble() / duration * height).roundToInt()
        val placements = measurables.map { measurable ->
            when (val data = measurable.parentData) {
                TimelineData -> Placement(measurable.measure(Constraints.fixed(axisWidth, height)), 0, 0)
                is CardData -> {
                    // Read snapshot state here, not during composition: animated bounds trigger measurement.
                    val start = data.startTime()
                    val end = data.endTime()
                    require(start >= startTime && end <= endTime && start < end) {
                        "card time must satisfy CourseLayout.startTime <= start < end <= CourseLayout.endTime."
                    }
                    val column = allowIsoWeekNumber.indexOf(data.day)
                    val left = axisWidth + ((width - axisWidth).toDouble() * column / allowIsoWeekNumber.size).roundToInt()
                    val right = axisWidth + ((width - axisWidth).toDouble() * (column + 1) / allowIsoWeekNumber.size).roundToInt()
                    val top = y(start)
                    Placement(measurable.measure(Constraints.fixed(right - left, y(end) - top)), left, top)
                }
                else -> error("CourseLayout content must use timeline() or card().")
            }
        }
        layout(width, height) {
            placements.forEach { it.placeable.place(it.x, it.y) }
        }
    }
}

private data class Placement(val placeable: androidx.compose.ui.layout.Placeable, val x: Int, val y: Int)

interface CourseLayoutScope {
    /** Optional left column. Only one timeline may be declared. */
    @Composable
    fun timeline(content: @Composable CourseTimelineScope.() -> Unit)

    /** Providers are evaluated during measurement. Keep them pure and read animated State inside them. */
    @Composable
    fun card(
        isoWeekNumber: Int,
        startTime: () -> LocalTime,
        endTime: () -> LocalTime,
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    )

    @Composable
    fun card(
        isoWeekNumber: Int,
        startTime: LocalTime,
        endTime: LocalTime,
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) = card(isoWeekNumber, { startTime }, { endTime }, modifier, content)
}

/** Coordinates have the same origin and scale as cards. No label padding is added. */
interface CourseTimelineScope : BoxScope {
    val startTime: LocalTime
    val endTime: LocalTime
    fun offsetOf(time: LocalTime): Dp
}

private class CourseLayoutScopeImpl(
    private val start: LocalTime,
    private val end: LocalTime,
    private val days: List<Int>,
) : CourseLayoutScope {
    @Composable
    override fun timeline(content: @Composable CourseTimelineScope.() -> Unit) {
        BoxWithConstraints(Modifier.then(SlotDataModifier(TimelineData)), propagateMinConstraints = true) {
            val height = maxHeight
            Box(Modifier.fillMaxSize()) {
                val scope = object : CourseTimelineScope, BoxScope by this {
                    override val startTime = start
                    override val endTime = end
                    override fun offsetOf(time: LocalTime): Dp = height *
                        ((time.toNanosecondOfDay() - start.toNanosecondOfDay()).toDouble() /
                            (end.toNanosecondOfDay() - start.toNanosecondOfDay())).toFloat()
                }
                scope.content()
            }
        }
    }

    @Composable
    override fun card(
        isoWeekNumber: Int,
        startTime: () -> LocalTime,
        endTime: () -> LocalTime,
        modifier: Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        if (isoWeekNumber !in days) return
        Box(
            modifier.then(SlotDataModifier(CardData(isoWeekNumber, startTime, endTime))),
            propagateMinConstraints = true,
            content = content,
        )
    }
}

private object TimelineData
private data class CardData(val day: Int, val startTime: () -> LocalTime, val endTime: () -> LocalTime)
private data class SlotDataModifier(val data: Any) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = data
}
