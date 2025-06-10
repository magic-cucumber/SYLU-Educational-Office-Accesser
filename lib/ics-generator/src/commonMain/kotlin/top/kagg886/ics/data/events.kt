package top.kagg886.ics.data

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlin.time.Duration

/**
 * ICS 日历数据类
 */
data class IcsCalendar(
    val version: String = "2.0",
    val prodId: String = "-//Kagg886//ICS Generator//EN",
    val calScale: String = "GREGORIAN",
    val method: String? = null,
    val events: MutableList<IcsEvent> = mutableListOf(),
)

/**
 * ICS 事件数据类
 */
data class IcsEvent(
    val uid: String,
    val timeZone: TimeZone,
    val dtStamp: LocalDateTime,
    val dtStart: LocalDateTime,
    val dtEnd: LocalDateTime? = null,
    val duration: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val organizer: String? = null,
    val attendees: MutableList<String> = mutableListOf(),
    val categories: MutableList<String> = mutableListOf(),
    val status: EventStatus? = null,
    val priority: Int? = null,
    val url: String? = null,
    val recurrenceRule: String? = null,
    val recurrenceId: LocalDateTime? = null,
    val exceptionDates: MutableList<LocalDateTime> = mutableListOf(),
    val alarms: MutableList<IcsAlarm> = mutableListOf(),
    val isAllDay: Boolean = false,
    val transparency: Transparency? = null,
    val classification: Classification? = null
)

/**
 * ICS 提醒数据类
 */
data class IcsAlarm(
    val action: AlarmAction,
    val trigger: String, // 例如: "-PT15M" 表示提前15分钟
    val description: String? = null,
    val summary: String? = null,
    val attendees: MutableList<String> = mutableListOf(),
    val duration: String? = null,
    val repeat: Int? = null
)

/**
 * 事件状态枚举
 */
enum class EventStatus {
    TENTATIVE,
    CONFIRMED,
    CANCELLED
}

/**
 * 提醒动作枚举
 */
enum class AlarmAction {
    AUDIO,
    DISPLAY,
    EMAIL
}

/**
 * 透明度枚举
 */
enum class Transparency {
    OPAQUE,
    TRANSPARENT
}

/**
 * 分类枚举
 */
enum class Classification {
    PUBLIC,
    PRIVATE,
    CONFIDENTIAL
}

/**
 * 重复规则建造者
 */
data class RecurrenceRule(
    val frequency: Frequency,
    val interval: Int? = null,
    val count: Int? = null,
    val until: LocalDateTime? = null,
    val byDay: List<String>? = null,
    val byMonth: List<Int>? = null,
    val byMonthDay: List<Int>? = null,
    val byYearDay: List<Int>? = null,
    val byWeekNo: List<Int>? = null,
    val byHour: List<Int>? = null,
    val byMinute: List<Int>? = null,
    val bySecond: List<Int>? = null,
    val weekStart: String? = null
) {
    fun toRRule(): String {
        val parts = mutableListOf<String>()
        parts.add("FREQ=${frequency.name}")
        interval?.let { parts.add("INTERVAL=$it") }
        count?.let { parts.add("COUNT=$it") }
        until?.let { parts.add("UNTIL=${formatDateTime(it)}") }
        byDay?.let { if (it.isNotEmpty()) parts.add("BYDAY=${it.joinToString(",")}") }
        byMonth?.let { if (it.isNotEmpty()) parts.add("BYMONTH=${it.joinToString(",")}") }
        byMonthDay?.let { if (it.isNotEmpty()) parts.add("BYMONTHDAY=${it.joinToString(",")}") }
        byYearDay?.let { if (it.isNotEmpty()) parts.add("BYYEARDAY=${it.joinToString(",")}") }
        byWeekNo?.let { if (it.isNotEmpty()) parts.add("BYWEEKNO=${it.joinToString(",")}") }
        byHour?.let { if (it.isNotEmpty()) parts.add("BYHOUR=${it.joinToString(",")}") }
        byMinute?.let { if (it.isNotEmpty()) parts.add("BYMINUTE=${it.joinToString(",")}") }
        bySecond?.let { if (it.isNotEmpty()) parts.add("BYSECOND=${it.joinToString(",")}") }
        weekStart?.let { parts.add("WKST=$it") }
        return parts.joinToString(";")
    }
}

/**
 * 频率枚举
 */
enum class Frequency {
    SECONDLY,
    MINUTELY,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * 格式化日期时间为 ICS 格式
 *
 * 根据 RFC 5545 规范将 LocalDateTime 转换为 ICS 格式的日期时间字符串。
 *
 * @param dateTime 要格式化的日期时间
 * @param isAllDay 是否为全天事件，true 时只返回日期部分
 * @return ICS 格式的日期时间字符串
 * - 普通事件：YYYYMMDDTHHMMSS
 * - 全天事件：YYYYMMDD
 */
internal fun formatDateTime(dateTime: LocalDateTime, isAllDay: Boolean = false): String = dateTime.format(
    LocalDateTime.Format {
        year()
        monthNumber()
        dayOfMonth()
        if (!isAllDay) {
            char('T')
            hour()
            minute()
            second()
        }
    }
)

/**
 * 格式化文本，处理 ICS 格式中的特殊字符
 *
 * 根据 RFC 5545 规范对文本进行转义处理，确保特殊字符能够正确显示。
 *
 * @param text 要格式化的原始文本
 * @return 转义后的文本，可安全用于 ICS 格式
 *
 * 转义规则：
 * - \ -> \\
 * - , -> \,
 * - ; -> \;
 * - \n -> \n
 * - \r -> \r
 */
internal fun formatText(text: String): String {
    return text
        .replace("\\", "\\\\")
        .replace(",", "\\,")
        .replace(";", "\\;")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

/**
 * 将 Kotlin Duration 转换为 ISO 8601 格式的字符串
 * 
 * 用于 ICS 提醒的 trigger 时间和事件的 duration 字段。
 * 支持负数表示事件开始前的时间。
 * 
 * @param duration Kotlin Duration 对象
 * @param isNegative 是否为负数（事件开始前），默认为 false
 * @return ISO 8601 格式的持续时间字符串
 * 
 * 示例：
 * - Duration.parse("PT15M") -> "PT15M"
 * - Duration.parse("PT1H") -> "PT1H"  
 * - Duration.parse("P1D") -> "P1D"
 * - 负数时间：formatDuration(Duration.parse("PT15M"), true) -> "-PT15M"
 */
internal fun formatDuration(duration: Duration, isNegative: Boolean = false): String {
    return duration.toComponents { days, hours, minutes, seconds, _ ->
        val result = StringBuilder()
        
        // 添加负号（如果需要）
        if (isNegative) result.append("-")
        
        // ISO 8601 格式总是以 P 开头
        result.append("P")
        
        // 添加日期部分
        if (days > 0) {
            result.append("${days}D")
        }
        
        // 确定是否需要时间部分
        val hasTime = hours > 0 || minutes > 0 || seconds > 0 || (days == 0L)
        
        if (hasTime) {
            result.append("T")
            
            // 添加小时
            if (hours > 0) {
                result.append("${hours}H")
            }
            
            // 添加分钟
            if (minutes > 0) {
                result.append("${minutes}M")
            }
            
            // 添加秒（如果有秒，或者这是一个零持续时间）
            if (seconds > 0 || (days == 0L && hours == 0 && minutes == 0)) {
                result.append("${seconds}S")
            }
        }
        
        result.toString()
    }
}

/**
 * 创建提醒触发时间的便捷函数
 *
 * @param duration 时间间隔
 * @param beforeEvent 是否在事件开始前（true）还是开始后（false），默认为 true
 * @return 格式化的触发时间字符串
 *
 * @sample
 * ```kotlin
 * // 事件开始前15分钟
 * triggerTime(15.minutes)
 *
 * // 事件开始前1小时
 * triggerTime(1.hours)
 *
 * // 事件开始时
 * triggerTime(0.seconds, beforeEvent = false)
 *
 * // 事件开始后5分钟
 * triggerTime(5.minutes, beforeEvent = false)
 * ```
 */
fun triggerTime(duration: Duration, beforeEvent: Boolean = true): String {
    return formatDuration(duration, beforeEvent)
}

