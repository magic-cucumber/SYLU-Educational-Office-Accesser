import top.kagg886.ics.data.formatDuration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDurationTest {
    
    @Test
    fun testBasicDurations() {
        // 测试基本的时间单位
        assertEquals("PT15M", formatDuration(15.minutes))
        assertEquals("PT1H", formatDuration(1.hours))
        assertEquals("P1D", formatDuration(1.days))
        assertEquals("PT30S", formatDuration(30.seconds))
    }
    
    @Test
    fun testCombinedDurations() {
        // 测试组合时间
        assertEquals("PT1H30M", formatDuration(1.hours + 30.minutes))
        assertEquals("PT1H30M45S", formatDuration(1.hours + 30.minutes + 45.seconds))
        assertEquals("P1DT2H30M", formatDuration(1.days + 2.hours + 30.minutes))
        assertEquals("P2DT1H", formatDuration(2.days + 1.hours))
    }
    
    @Test
    fun testNegativeDurations() {
        // 测试负数（事件开始前）
        assertEquals("-PT15M", formatDuration(15.minutes, isNegative = true))
        assertEquals("-PT1H", formatDuration(1.hours, isNegative = true))
        assertEquals("-P1D", formatDuration(1.days, isNegative = true))
        assertEquals("-PT1H30M", formatDuration(1.hours + 30.minutes, isNegative = true))
    }
    
    @Test
    fun testZeroDuration() {
        // 测试零持续时间
        assertEquals("PT0S", formatDuration(Duration.ZERO))
        assertEquals("PT0S", formatDuration(Duration.ZERO, isNegative = false))
    }
    
    @Test
    fun testComplexDurations() {
        // 测试复杂的时间组合
        assertEquals("P3DT4H5M6S", formatDuration(3.days + 4.hours + 5.minutes + 6.seconds))
        assertEquals("PT1H30M", formatDuration(90.minutes)) // 90分钟
        assertEquals("PT2H", formatDuration(7200.seconds)) // 2小时的秒数表示
    }
    
    @Test 
    fun testOnlyDays() {
        // 测试只有天数的情况
        assertEquals("P7D", formatDuration(7.days))
        assertEquals("P365D", formatDuration(365.days))
    }
    
    @Test
    fun testOnlyTime() {
        // 测试只有时间部分的情况
        assertEquals("PT5H", formatDuration(5.hours))
        assertEquals("PT45M", formatDuration(45.minutes))
        assertEquals("PT2M", formatDuration(120.seconds))
    }
} 