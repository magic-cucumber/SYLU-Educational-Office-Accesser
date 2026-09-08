package top.kagg886.eoa.component.course

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CourseLayoutTest {
    @Test
    fun timelineAndCardsShareTimeCoordinates() = runComposeUiTest {
        var labelY = 0f
        var cardY = 0f
        setContent {
            CourseLayout(LocalTime(8, 0), LocalTime(10, 0), Modifier.size(300.dp, 240.dp)) {
                timeline {
                    Box(Modifier.offset(y = offsetOf(LocalTime(8, 45))).size(1.dp)
                        .onGloballyPositioned { labelY = it.positionInRoot().y })
                }
                card(1, LocalTime(8, 45), LocalTime(9, 0), Modifier.onGloballyPositioned {
                    cardY = it.positionInRoot().y
                }) {}
            }
        }
        runOnIdle { assertEquals(cardY, labelY, 1f) }
    }

    @Test
    fun animatedBoundsAreReadDuringMeasurement() = runComposeUiTest {
        val end = mutableStateOf(LocalTime(9, 0))
        var compositions = 0
        var layoutSize = IntSize.Zero
        var cardSize = IntSize.Zero
        var cardPosition = Offset.Zero
        setContent {
            CourseLayout(
                LocalTime(8, 0), LocalTime(10, 0),
                Modifier.size(300.dp, 240.dp).onGloballyPositioned { layoutSize = it.size },
                allowIsoWeekNumber = listOf(5, 1),
                timelineWidth = 60.dp,
            ) {
                SideEffect { compositions++ }
                card(1, { LocalTime(8, 30) }, { end.value }, Modifier.onGloballyPositioned {
                    cardSize = it.size
                    cardPosition = it.positionInParent()
                }) { Box(Modifier) }
            }
        }
        runOnIdle {
            assertEquals(layoutSize.height / 4, cardSize.height)
            assertEquals(layoutSize.height / 4f, cardPosition.y)
            assertEquals(layoutSize.width * 0.6f, cardPosition.x, 1f)
            assertEquals((layoutSize.width * 0.4f).toInt(), cardSize.width)
        }
        val previousCompositions = runOnIdle { compositions }
        runOnIdle { end.value = LocalTime(9, 30) }
        runOnIdle {
            assertEquals(layoutSize.height / 2, cardSize.height)
            assertEquals(previousCompositions, compositions)
        }
    }

    @Test
    fun explicitHeightWorksInsideScrollAndHiddenDaysAreNotComposed() = runComposeUiTest {
        var viewportHeight = 0
        var contentHeight = 0
        setContent {
            Column(Modifier.size(300.dp, 200.dp).onGloballyPositioned { viewportHeight = it.size.height }
                .verticalScroll(rememberScrollState())) {
                CourseLayout(
                    LocalTime(8, 0), LocalTime(10, 0),
                    Modifier.height(800.dp).onGloballyPositioned { contentHeight = it.size.height },
                ) {
                    card(7, LocalTime(8, 0), LocalTime(9, 0)) {
                        error("A hidden weekday must not be composed")
                    }
                }
            }
        }
        runOnIdle { assertEquals(viewportHeight * 4, contentHeight) }
    }

    @Test
    fun unboundedHeightExplainsRequiredHeight() {
        val error = assertFailsWith<IllegalArgumentException> {
            runComposeUiTest {
                setContent {
                    Layout(content = {
                        CourseLayout(LocalTime(8, 0), LocalTime(10, 0)) {}
                    }) { children, constraints ->
                        val child = children.single().measure(constraints.copy(maxHeight = Constraints.Infinity))
                        layout(child.width, child.height) { child.place(0, 0) }
                    }
                }
                waitForIdle()
            }
        }
        assertTrue(error.message.orEmpty().contains("Modifier.height"))
    }

    @Test
    fun duplicateTimelinesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            runComposeUiTest {
                setContent {
                    CourseLayout(LocalTime(8, 0), LocalTime(10, 0), Modifier.size(300.dp)) {
                        timeline {}
                        timeline {}
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun invalidDaysAreRejected() {
        for (days in listOf(emptyList(), listOf(1, 1), listOf(0), listOf(8))) {
            assertFailsWith<IllegalArgumentException> { validateDays(days) }
        }
    }
}
