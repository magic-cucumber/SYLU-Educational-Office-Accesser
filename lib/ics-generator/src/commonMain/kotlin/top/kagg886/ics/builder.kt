package top.kagg886.ics

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import top.kagg886.ics.data.*
import top.kagg886.ics.util.IcsWriter
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ICS 日历构建器，用于创建符合 RFC 5545 标准的 iCalendar 格式日历文件
 *
 * 支持的功能：
 * - 设置日历元数据（产品标识、方法等）
 * - 添加各种类型的事件（会议、全天事件、重复事件等）
 * - 自动生成符合 RFC 5545 规范的 ICS 格式输出
 *
 * @sample
 * ```kotlin
 * ics {
 *     writeTo { icsContent -> println(icsContent) }
 *     prodId("-//MyCompany//MyApp//EN")
 *
 *     event {
 *         summary("团队会议")
 *         startTime(LocalDateTime(2024, 1, 15, 10, 0))
 *         endTime(LocalDateTime(2024, 1, 15, 11, 30))
 *     }
 * }
 * ```
 */
class IcsBuilder internal constructor() {
    private var writeTo by Delegates.notNull<IcsWriter>()
    private var calendar = IcsCalendar()

    /**
     * 设置 ICS 内容的输出目标
     *
     * @param writer IcsWriter 函数，接收生成的 ICS 字符串内容
     *
     * ```kotlin
     * ics {
     *     writeTo { icsContent ->
     *         println(icsContent)
     *         // 或者写入文件
     *         File("calendar.ics").writeText(icsContent)
     *     }
     * }
     * ```
     */
    fun writeTo(writer: IcsWriter) {
        writeTo = writer
    }

    /**
     * 设置日历产品标识符 (PRODID)
     *
     * 产品标识符用于标识创建此日历数据的软件产品。
     * 格式通常为："-//Company//Product//Language"
     *
     * @param prodId 产品标识符字符串，默认为 "-//Kagg886//ICS Generator//EN"
     *
     * ```kotlin
     * ics {
     *     prodId("-//MyCompany//MyCalendarApp//EN")
     *     // ... 其他配置
     * }
     * ```
     */
    fun prodId(prodId: String) {
        calendar = calendar.copy(prodId = prodId)
    }

    /**
     * 设置日历方法 (METHOD)
     *
     * 指定 iCalendar 对象的用途，常用值包括：
     * - "PUBLISH" - 发布日历信息
     * - "REQUEST" - 请求/邀请
     * - "REPLY" - 回复邀请
     * - "CANCEL" - 取消事件
     *
     * @param method 方法字符串
     *
     * ```kotlin
     * ics {
     *     method("PUBLISH")
     *     // ... 其他配置
     * }
     * ```
     */
    fun method(method: String) {
        calendar = calendar.copy(method = method)
    }

    /**
     * 添加一个事件到日历中
     *
     * 使用 EventBuilder DSL 构建事件，支持设置事件的各种属性，
     * 包括时间、地点、参与者、提醒、重复规则等。
     *
     * @param block EventBuilder 的 DSL 配置块
     *
     * ```kotlin
     * ics {
     *     event {
     *         summary("项目会议")
     *         description("讨论项目进度")
     *         location("会议室A")
     *         startTime(LocalDateTime(2024, 1, 15, 14, 0))
     *         endTime(LocalDateTime(2024, 1, 15, 16, 0))
     *         organizer("mailto:pm@company.com")
     *         attendee("mailto:dev@company.com")
     *
     *         alarm {
     *             action(AlarmAction.DISPLAY)
     *             trigger("-PT15M")
     *             description("会议提醒")
     *         }
     *     }
     * }
     * ```
     */
    fun event(block: EventBuilder.() -> Unit) {
        val eventBuilder = EventBuilder()
        eventBuilder.block()
        calendar.events.add(eventBuilder.build())
    }

    /**
     * 构建并输出 ICS 内容
     */
    internal fun build() {
        val icsContent = generateIcsContent(calendar)
        writeTo.writeTo(icsContent)
    }

    /**
     * 生成 ICS 内容
     */
    private fun generateIcsContent(calendar: IcsCalendar): String {
        val builder = StringBuilder()

        // 日历开始
        builder.appendLine("BEGIN:VCALENDAR")
        builder.appendLine("VERSION:${calendar.version}")
        builder.appendLine("PRODID:${calendar.prodId}")
        builder.appendLine("CALSCALE:${calendar.calScale}")

        calendar.method?.let { builder.appendLine("METHOD:$it") }

        // 添加事件
        calendar.events.forEach { event ->
            builder.append(generateEventContent(event))
        }

        // 日历结束
        builder.appendLine("END:VCALENDAR")

        return builder.toString()
    }

    /**
     * 生成事件内容
     */
    private fun generateEventContent(event: IcsEvent): String {

        val builder = StringBuilder()

        builder.appendLine("BEGIN:VEVENT")
        builder.appendLine("UID:${event.uid}")
        builder.appendLine("DTSTAMP:${formatDateTime(event.dtStamp)}Z")

        if (event.isAllDay) {
            // 全天事件只使用日期，不涉及时区
            builder.appendLine("DTSTART;VALUE=DATE:${formatDateTime(event.dtStart, true)}")
            event.dtEnd?.let {
                builder.appendLine("DTEND;VALUE=DATE:${formatDateTime(it, true)}")
            }
        } else {
            // 根据时区情况格式化时间
            val timeZoneId = event.timeZone.id
            if (timeZoneId == "UTC" || timeZoneId == "Z") {
                // UTC 时间使用 Z 后缀
                builder.appendLine("DTSTART:${formatDateTime(event.dtStart)}Z")
                event.dtEnd?.let {
                    builder.appendLine("DTEND:${formatDateTime(it)}Z")
                }
            } else {
                // 其他时区使用 TZID 参数
                builder.appendLine("DTSTART;TZID=${timeZoneId}:${formatDateTime(event.dtStart)}")
                event.dtEnd?.let {
                    builder.appendLine("DTEND;TZID=${timeZoneId}:${formatDateTime(it)}")
                }
            }
        }

        event.duration?.let { builder.appendLine("DURATION:$it") }
        event.summary?.let { builder.appendLine("SUMMARY:${formatText(it)}") }
        event.description?.let { builder.appendLine("DESCRIPTION:${formatText(it)}") }
        event.location?.let { builder.appendLine("LOCATION:${formatText(it)}") }
        event.organizer?.let { builder.appendLine("ORGANIZER:$it") }
        event.status?.let { builder.appendLine("STATUS:${it.name}") }
        event.priority?.let { builder.appendLine("PRIORITY:$it") }
        event.url?.let { builder.appendLine("URL:$it") }
        event.transparency?.let { builder.appendLine("TRANSP:${it.name}") }
        event.classification?.let { builder.appendLine("CLASS:${it.name}") }

        // 添加参与者
        event.attendees.forEach { attendee ->
            builder.appendLine("ATTENDEE:$attendee")
        }

        // 添加分类
        if (event.categories.isNotEmpty()) {
            builder.appendLine("CATEGORIES:${event.categories.joinToString(",") { formatText(it) }}")
        }

        // 重复规则
        event.recurrenceRule?.let { builder.appendLine("RRULE:$it") }
        event.recurrenceId?.let {
            val timeZoneId = event.timeZone.id
            if (timeZoneId == "UTC" || timeZoneId == "Z") {
                builder.appendLine("RECURRENCE-ID:${formatDateTime(it)}Z")
            } else {
                builder.appendLine("RECURRENCE-ID;TZID=${timeZoneId}:${formatDateTime(it)}")
            }
        }

        // 例外日期
        if (event.exceptionDates.isNotEmpty()) {
            val timeZoneId = event.timeZone.id
            if (timeZoneId == "UTC" || timeZoneId == "Z") {
                val exDates = event.exceptionDates.joinToString(",") { "${formatDateTime(it)}Z" }
                builder.appendLine("EXDATE:$exDates")
            } else {
                val exDates = event.exceptionDates.joinToString(",") { formatDateTime(it) }
                builder.appendLine("EXDATE;TZID=${timeZoneId}:$exDates")
            }
        }

        // 提醒
        event.alarms.forEach { alarm ->
            builder.append(generateAlarmContent(alarm))
        }

        builder.appendLine("END:VEVENT")

        return builder.toString()
    }

    /**
     * 生成提醒内容
     */
    private fun generateAlarmContent(alarm: IcsAlarm): String {
        val builder = StringBuilder()

        builder.appendLine("BEGIN:VALARM")
        builder.appendLine("ACTION:${alarm.action.name}")
        builder.appendLine("TRIGGER:${alarm.trigger}")

        alarm.description?.let { builder.appendLine("DESCRIPTION:${formatText(it)}") }
        alarm.summary?.let { builder.appendLine("SUMMARY:${formatText(it)}") }
        alarm.duration?.let { builder.appendLine("DURATION:$it") }
        alarm.repeat?.let { builder.appendLine("REPEAT:$it") }

        alarm.attendees.forEach { attendee ->
            builder.appendLine("ATTENDEE:$attendee")
        }

        builder.appendLine("END:VALARM")

        return builder.toString()
    }
}

/**
 * 事件构建器，用于创建单个日历事件
 *
 * 支持配置事件的所有标准属性：
 * - 基本信息：标题、描述、位置、时间
 * - 参与者：组织者、参与者列表
 * - 分类：状态、优先级、分类标签
 * - 重复：重复规则、例外日期
 * - 提醒：多种提醒方式和时间
 *
 * @sample
 * ```kotlin
 * event {
 *     summary("重要会议")
 *     description("项目进度讨论")
 *     location("会议室A")
 *     startTime(LocalDateTime(2024, 1, 15, 14, 0))
 *     endTime(LocalDateTime(2024, 1, 15, 16, 0))
 *     status(EventStatus.CONFIRMED)
 *     priority(1)
 *
 *     alarm {
 *         action(AlarmAction.DISPLAY)
 *         trigger("-PT15M")
 *     }
 * }
 * ```
 */
class EventBuilder internal constructor() {
    private var uid: String = generateUID()
    private var timeZone: TimeZone = TimeZone.currentSystemDefault()
    @OptIn(ExperimentalTime::class)
    private var dtStamp: LocalDateTime = Clock.System.now().toLocalDateTime(timeZone)
    private var dtStart: LocalDateTime by Delegates.notNull()
    private var dtEnd: LocalDateTime? = null
    private var duration: String? = null
    private var summary: String? = null
    private var description: String? = null
    private var location: String? = null
    private var organizer: String? = null
    private val attendees = mutableListOf<String>()
    private val categories = mutableListOf<String>()
    private var status: EventStatus? = null
    private var priority: Int? = null
    private var url: String? = null
    private var recurrenceRule: String? = null
    private var recurrenceId: LocalDateTime? = null
    private val exceptionDates = mutableListOf<LocalDateTime>()
    private val alarms = mutableListOf<IcsAlarm>()
    private var isAllDay: Boolean = false
    private var transparency: Transparency? = null
    private var classification: Classification? = null

    /**
     * 设置事件唯一标识符 (UID)
     *
     * 如果不设置，系统会自动生成一个唯一的 UID。
     * UID 在整个日历系统中应该是全局唯一的。
     *
     * @param uid 唯一标识符字符串，建议包含域名以确保全局唯一性
     *
     * ```kotlin
     * event {
     *     uid("meeting-20240115@company.com")
     *     // ... 其他属性
     * }
     * ```
     */
    fun uid(uid: String) {
        this.uid = uid
    }

    fun timeZone(timeZone: TimeZone) {
        this.timeZone = timeZone
    }

    /**
     * 设置事件开始时间 (DTSTART)
     *
     * 这是必需的属性，所有事件都必须有开始时间。
     *
     * @param dateTime 事件开始的日期和时间
     *
     * ```kotlin
     * event {
     *     startTime(LocalDateTime(2024, 1, 15, 14, 0)) // 2024年1月15日下午2点
     *     // ... 其他属性
     * }
     * ```
     */
    fun startTime(dateTime: LocalDateTime) {
        this.dtStart = dateTime
    }

    /**
     * 设置事件结束时间 (DTEND)
     *
     * 对于有明确结束时间的事件，应设置此属性。
     * 注意：不能同时设置 endTime 和 duration。
     *
     * @param dateTime 事件结束的日期和时间
     *
     * ```kotlin
     * event {
     *     startTime(LocalDateTime(2024, 1, 15, 14, 0))
     *     endTime(LocalDateTime(2024, 1, 15, 16, 0)) // 持续2小时
     *     // ... 其他属性
     * }
     * ```
     */
    fun endTime(dateTime: LocalDateTime) {
        this.dtEnd = dateTime
    }

    /**
     * 设置事件持续时间 (DURATION)
     *
     * 使用 ISO 8601 持续时间格式，如："PT1H30M" 表示1小时30分钟。
     * 注意：不能同时设置 endTime 和 duration。
     *
     * @param duration ISO 8601 格式的持续时间字符串
     *
     * ```kotlin
     * event {
     *     startTime(LocalDateTime(2024, 1, 15, 14, 0))
     *     duration("PT2H") // 持续2小时
     *     // 或者
     *     duration("PT1H30M") // 持续1小时30分钟
     * }
     * ```
     */
    fun duration(duration: String) {
        this.duration = duration
    }

    /**
     * 设置事件持续时间（便捷方法）
     *
     * 使用 Kotlin Duration 对象来设置事件持续时间，自动转换为 ISO 8601 格式。
     * 注意：不能同时设置 endTime 和 duration。
     *
     * @param duration Kotlin Duration 对象
     *
     * ```kotlin
     * import kotlin.time.Duration.Companion.minutes
     * import kotlin.time.Duration.Companion.hours
     *
     * event {
     *     startTime(LocalDateTime(2024, 1, 15, 14, 0))
     *
     *     // 持续2小时
     *     duration(2.hours)
     *
     *     // 持续1小时30分钟
     *     duration(90.minutes)
     *
     *     // 持续30分钟
     *     duration(30.minutes)
     * }
     * ```
     */
    fun duration(duration: Duration) {
        this.duration = formatDuration(duration, false)
    }

    /**
     * 设置事件摘要/标题 (SUMMARY)
     *
     * 事件的简短描述性标题，通常显示在日历视图中。
     *
     * @param summary 事件标题文本
     *
     * ```kotlin
     * event {
     *     summary("团队会议")
     *     // ... 其他属性
     * }
     * ```
     */
    fun summary(summary: String) {
        this.summary = summary
    }

    /**
     * 设置事件详细描述 (DESCRIPTION)
     *
     * 事件的详细说明，可以包含多行文本和具体信息。
     *
     * @param description 事件的详细描述文本
     *
     * ```kotlin
     * event {
     *     summary("项目会议")
     *     description("讨论Q1项目进度\n\n议程：\n1. 进度汇报\n2. 问题讨论\n3. 下一步计划")
     *     // ... 其他属性
     * }
     * ```
     */
    fun description(description: String) {
        this.description = description
    }

    /**
     * 设置事件地点 (LOCATION)
     *
     * 事件举办的物理或虚拟地点。
     *
     * @param location 地点描述，可以是具体地址、会议室名称或在线会议链接
     *
     * ```kotlin
     * event {
     *     summary("季度总结会")
     *     location("北京市朝阳区xx大厦22楼会议室A")
     *     // 或者在线会议
     *     location("Zoom会议：https://zoom.us/j/123456789")
     * }
     * ```
     */
    fun location(location: String) {
        this.location = location
    }

    /**
     * 设置事件组织者 (ORGANIZER)
     *
     * 指定事件的组织者，通常包含邮箱地址和可选的显示名称。
     *
     * @param organizer 组织者信息，格式如："mailto:user@domain.com" 或 "CN=姓名:mailto:user@domain.com"
     *
     * ```kotlin
     * event {
     *     summary("项目启动会")
     *     organizer("mailto:pm@company.com")
     *     // 或者包含显示名称
     *     organizer("CN=项目经理:mailto:pm@company.com")
     * }
     * ```
     */
    fun organizer(organizer: String) {
        this.organizer = organizer
    }

    /**
     * 添加事件参与者 (ATTENDEE)
     *
     * 可以多次调用此函数来添加多个参与者。
     *
     * @param attendee 参与者信息，格式如："mailto:user@domain.com" 或 "CN=姓名:mailto:user@domain.com"
     *
     * ```kotlin
     * event {
     *     summary("团队会议")
     *     organizer("mailto:manager@company.com")
     *     attendee("mailto:dev1@company.com")
     *     attendee("CN=张三:mailto:zhangsan@company.com")
     *     attendee("mailto:qa@company.com")
     * }
     * ```
     */
    fun attendee(attendee: String) {
        this.attendees.add(attendee)
    }

    /**
     * 设置事件状态 (STATUS)
     *
     * 指定事件的当前状态。
     *
     * @param status 事件状态枚举值
     * - EventStatus.TENTATIVE: 暂定
     * - EventStatus.CONFIRMED: 已确认
     * - EventStatus.CANCELLED: 已取消
     *
     * ```kotlin
     * event {
     *     summary("重要会议")
     *     status(EventStatus.CONFIRMED)
     *     // ... 其他属性
     * }
     * ```
     */
    fun status(status: EventStatus) {
        this.status = status
    }

    /**
     * 设置事件优先级 (PRIORITY)
     *
     * 优先级用数字表示，范围 0-9。
     *
     * @param priority 优先级数字
     * - 0: 未定义
     * - 1: 最高优先级
     * - 5: 中等优先级
     * - 9: 最低优先级
     *
     * ```kotlin
     * event {
     *     summary("紧急会议")
     *     priority(1) // 最高优先级
     *     // ... 其他属性
     * }
     * ```
     */
    fun priority(priority: Int) {
        this.priority = priority
    }

    /**
     * 设置事件相关 URL (URL)
     *
     * 指向与事件相关的网页或资源的链接。
     *
     * @param url 相关链接地址
     *
     * ```kotlin
     * event {
     *     summary("在线培训")
     *     url("https://training.company.com/session/123")
     *     // ... 其他属性
     * }
     * ```
     */
    fun url(url: String) {
        this.url = url
    }

    /**
     * 添加事件分类标签 (CATEGORIES)
     *
     * 可以多次调用此函数来添加多个分类标签。
     *
     * @param category 分类标签文本
     *
     * ```kotlin
     * event {
     *     summary("项目会议")
     *     category("工作")
     *     category("重要")
     *     category("项目管理")
     * }
     * ```
     */
    fun category(category: String) {
        this.categories.add(category)
    }

    /**
     * 设置为全天事件
     *
     * 全天事件没有具体的时间，只有日期。
     *
     * @param allDay 是否为全天事件，默认为 true
     *
     * ```kotlin
     * event {
     *     summary("公司年会")
     *     startTime(LocalDateTime(2024, 12, 31, 0, 0))
     *     allDay(true)
     *     // ... 其他属性
     * }
     * ```
     */
    fun allDay(allDay: Boolean = true) {
        this.isAllDay = allDay
    }

    /**
     * 设置事件透明度 (TRANSP)
     *
     * 指定事件是否在忙/闲时间查询中显示为忙碌状态。
     *
     * @param transparency 透明度枚举值
     * - Transparency.OPAQUE: 不透明，显示为忙碌状态
     * - Transparency.TRANSPARENT: 透明，不显示为忙碌状态
     *
     * ```kotlin
     * event {
     *     summary("个人时间")
     *     transparency(Transparency.TRANSPARENT) // 不显示为忙碌
     * }
     * ```
     */
    fun transparency(transparency: Transparency) {
        this.transparency = transparency
    }

    /**
     * 设置事件访问分类 (CLASS)
     *
     * 指定事件的隐私级别或访问限制。
     *
     * @param classification 分类枚举值
     * - Classification.PUBLIC: 公开
     * - Classification.PRIVATE: 私有
     * - Classification.CONFIDENTIAL: 机密
     *
     * ```kotlin
     * event {
     *     summary("董事会会议")
     *     classification(Classification.CONFIDENTIAL)
     * }
     * ```
     */
    fun classification(classification: Classification) {
        this.classification = classification
    }

    /**
     * 设置事件重复规则 (RRULE)
     *
     * 使用 RecurrenceBuilder DSL 来配置复杂的重复模式。
     *
     * @param block RecurrenceBuilder 的 DSL 配置块
     *
     * ```kotlin
     * event {
     *     summary("每周例会")
     *     startTime(LocalDateTime(2024, 1, 8, 10, 0))
     *
     *     recurrence {
     *         frequency(Frequency.WEEKLY)  // 每周重复
     *         count(10)                    // 重复10次
     *         byDay("MO")                  // 每周一
     *     }
     * }
     * ```
     */
    fun recurrence(block: RecurrenceBuilder.() -> Unit) {
        val recurrenceBuilder = RecurrenceBuilder()
        recurrenceBuilder.block()
        this.recurrenceRule = recurrenceBuilder.build().toRRule()
    }

    /**
     * 添加事件提醒 (VALARM)
     *
     * 可以多次调用此函数来添加多个提醒。
     *
     * @param block AlarmBuilder 的 DSL 配置块
     *
     * ```kotlin
     * event {
     *     summary("重要会议")
     *     startTime(LocalDateTime(2024, 1, 15, 14, 0))
     *
     *     // 提前15分钟提醒
     *     alarm {
     *         action(AlarmAction.DISPLAY)
     *         trigger("-PT15M")
     *         description("会议即将开始")
     *     }
     *
     *     // 提前1天邮件提醒
     *     alarm {
     *         action(AlarmAction.EMAIL)
     *         trigger("-P1D")
     *         summary("明天有重要会议")
     *     }
     * }
     * ```
     */
    fun alarm(block: AlarmBuilder.() -> Unit) {
        val alarmBuilder = AlarmBuilder()
        alarmBuilder.block()
        this.alarms.add(alarmBuilder.build())
    }

    /**
     * 添加重复事件的例外日期 (EXDATE)
     *
     * 指定在重复事件中应该跳过的特定日期。
     * 可以多次调用此函数来添加多个例外日期。
     *
     * @param dateTime 要排除的日期时间
     *
     * ```kotlin
     * event {
     *     summary("每周例会")
     *     startTime(LocalDateTime(2024, 1, 8, 10, 0))
     *
     *     recurrence {
     *         frequency(Frequency.WEEKLY)
     *         byDay("MO")
     *     }
     *
     *     // 春节期间跳过
     *     exceptionDate(LocalDateTime(2024, 2, 12, 10, 0))
     *     exceptionDate(LocalDateTime(2024, 2, 19, 10, 0))
     * }
     * ```
     */
    fun exceptionDate(dateTime: LocalDateTime) {
        this.exceptionDates.add(dateTime)
    }

    internal fun build(): IcsEvent {
        return IcsEvent(
            uid = uid,
            timeZone = timeZone,
            dtStamp = dtStamp,
            dtStart = dtStart,
            dtEnd = dtEnd,
            duration = duration,
            summary = summary,
            description = description,
            location = location,
            organizer = organizer,
            attendees = attendees,
            categories = categories,
            status = status,
            priority = priority,
            url = url,
            recurrenceRule = recurrenceRule,
            recurrenceId = recurrenceId,
            exceptionDates = exceptionDates,
            alarms = alarms,
            isAllDay = isAllDay,
            transparency = transparency,
            classification = classification
        )
    }

    /**
     * 生成唯一标识符
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateUID(): String = Uuid.random().toHexString()
}

/**
 * 重复规则构建器，用于创建复杂的事件重复模式
 *
 * 支持 RFC 5545 规范中定义的各种重复规则：
 * - 频率：秒、分、时、日、周、月、年
 * - 间隔：每 N 个时间单位重复一次
 * - 结束条件：重复次数或结束时间
 * - 细化条件：按星期几、按月份、按月中日期等
 *
 * @sample
 * ```kotlin
 * recurrence {
 *     frequency(Frequency.WEEKLY)  // 每周重复
 *     interval(2)                  // 每2周
 *     count(10)                    // 重复10次
 *     byDay("MO", "WE", "FR")      // 每周一、三、五
 * }
 * ```
 */
class RecurrenceBuilder internal constructor() {
    private var frequency: Frequency by Delegates.notNull()
    private var interval: Int? = null
    private var count: Int? = null
    private var until: LocalDateTime? = null
    private var byDay: List<String>? = null
    private var byMonth: List<Int>? = null
    private var byMonthDay: List<Int>? = null
    private var byYearDay: List<Int>? = null
    private var byWeekNo: List<Int>? = null
    private var byHour: List<Int>? = null
    private var byMinute: List<Int>? = null
    private var bySecond: List<Int>? = null
    private var weekStart: String? = null

    /**
     * 设置重复频率（必需）
     *
     * @param freq 重复频率枚举值
     * - Frequency.YEARLY: 每年
     * - Frequency.MONTHLY: 每月
     * - Frequency.WEEKLY: 每周
     * - Frequency.DAILY: 每日
     * - Frequency.HOURLY: 每小时
     * - Frequency.MINUTELY: 每分钟
     * - Frequency.SECONDLY: 每秒
     */
    fun frequency(freq: Frequency) { this.frequency = freq }

    /**
     * 设置重复间隔
     *
     * 指定每 N 个频率单位重复一次。
     *
     * @param interval 间隔数，必须为正整数，默认为 1
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.WEEKLY)
     *     interval(2) // 每2周重复一次
     * }
     * ```
     */
    fun interval(interval: Int) { this.interval = interval }

    /**
     * 设置重复次数
     *
     * 指定总共重复多少次。注意：不能同时设置 count 和 until。
     *
     * @param count 重复次数，必须为正整数
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.DAILY)
     *     count(30) // 重复30次
     * }
     * ```
     */
    fun count(count: Int) { this.count = count }

    /**
     * 设置重复结束时间
     *
     * 指定重复到什么时间结束。注意：不能同时设置 count 和 until。
     *
     * @param until 结束时间
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.WEEKLY)
     *     until(LocalDateTime(2024, 12, 31, 23, 59))
     * }
     * ```
     */
    fun until(until: LocalDateTime) { this.until = until }

    /**
     * 设置按星期几重复
     *
     * 指定在一周中的哪些天重复。
     *
     * @param days 星期几的缩写，可以包含数字前缀表示第几个
     * - "SU", "MO", "TU", "WE", "TH", "FR", "SA"
     * - "+1MO": 第一个周一
     * - "-1FR": 最后一个周五
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.WEEKLY)
     *     byDay("MO", "WE", "FR") // 每周一、三、五
     * }
     * ```
     */
    fun byDay(vararg days: String) { this.byDay = days.toList() }

    /**
     * 设置按月份重复
     *
     * 指定在一年中的哪些月份重复。
     *
     * @param months 月份数字，范围 1-12
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.YEARLY)
     *     byMonth(1, 7) // 每年1月和7月
     * }
     * ```
     */
    fun byMonth(vararg months: Int) { this.byMonth = months.toList() }

    /**
     * 设置按月中的第几天重复
     *
     * 指定在每月的第几天重复。
     *
     * @param days 日期数字，范围 1-31，负数表示从月末倒数
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.MONTHLY)
     *     byMonthDay(1, 15, -1) // 每月1号、15号和最后一天
     * }
     * ```
     */
    fun byMonthDay(vararg days: Int) { this.byMonthDay = days.toList() }

    /**
     * 设置按年中的第几天重复
     *
     * 指定在每年的第几天重复。
     *
     * @param days 年中的天数，范围 1-366，负数表示从年末倒数
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.YEARLY)
     *     byYearDay(1, 100, -1) // 每年第1天、第100天和最后一天
     * }
     * ```
     */
    fun byYearDay(vararg days: Int) { this.byYearDay = days.toList() }

    /**
     * 设置按年中的第几周重复
     *
     * 指定在每年的第几周重复。
     *
     * @param weeks 周数，范围 1-53，负数表示从年末倒数
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.YEARLY)
     *     byWeekNo(1, 26, -1) // 每年第1周、第26周和最后一周
     * }
     * ```
     */
    fun byWeekNo(vararg weeks: Int) { this.byWeekNo = weeks.toList() }

    /**
     * 设置按小时重复
     *
     * 指定在一天中的哪些小时重复。
     *
     * @param hours 小时数，范围 0-23
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.DAILY)
     *     byHour(9, 14, 18) // 每天上午9点、下午2点和6点
     * }
     * ```
     */
    fun byHour(vararg hours: Int) { this.byHour = hours.toList() }

    /**
     * 设置按分钟重复
     *
     * 指定在一小时中的哪些分钟重复。
     *
     * @param minutes 分钟数，范围 0-59
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.HOURLY)
     *     byMinute(0, 30) // 每小时的0分和30分
     * }
     * ```
     */
    fun byMinute(vararg minutes: Int) { this.byMinute = minutes.toList() }

    /**
     * 设置按秒重复
     *
     * 指定在一分钟中的哪些秒重复。
     *
     * @param seconds 秒数，范围 0-59
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.MINUTELY)
     *     bySecond(0, 30) // 每分钟的0秒和30秒
     * }
     * ```
     */
    fun bySecond(vararg seconds: Int) { this.bySecond = seconds.toList() }

    /**
     * 设置一周的开始日
     *
     * 指定星期的第一天，影响周相关的计算。
     *
     * @param day 星期几的缩写，默认为 "MO"
     * - "SU", "MO", "TU", "WE", "TH", "FR", "SA"
     *
     * ```kotlin
     * recurrence {
     *     frequency(Frequency.WEEKLY)
     *     weekStart("SU") // 以周日为一周的开始
     * }
     * ```
     */
    fun weekStart(day: String) { this.weekStart = day }

    internal fun build(): RecurrenceRule {
        return RecurrenceRule(
            frequency = frequency,
            interval = interval,
            count = count,
            until = until,
            byDay = byDay,
            byMonth = byMonth,
            byMonthDay = byMonthDay,
            byYearDay = byYearDay,
            byWeekNo = byWeekNo,
            byHour = byHour,
            byMinute = byMinute,
            bySecond = bySecond,
            weekStart = weekStart
        )
    }
}

/**
 * 提醒构建器，用于创建事件提醒
 *
 * 支持三种提醒类型：
 * - DISPLAY: 显示提醒（弹窗、通知等）
 * - AUDIO: 音频提醒（播放声音）
 * - EMAIL: 邮件提醒（发送邮件）
 *
 * 提醒时间使用 ISO 8601 持续时间格式：
 * - "-PT15M": 事件开始前15分钟
 * - "-P1D": 事件开始前1天
 * - "PT0S": 事件开始时
 *
 * @sample
 * ```kotlin
 * alarm {
 *     action(AlarmAction.DISPLAY)
 *     trigger("-PT15M")                // 提前15分钟
 *     description("会议即将开始")
 * }
 *
 * // 邮件提醒
 * alarm {
 *     action(AlarmAction.EMAIL)
 *     trigger("-P1D")                  // 提前1天
 *     summary("明天有重要会议")
 *     attendee("mailto:user@company.com")
 * }
 * ```
 */
class AlarmBuilder internal constructor() {
    private var action: AlarmAction by Delegates.notNull()
    private var trigger: String by Delegates.notNull()
    private var description: String? = null
    private var summary: String? = null
    private val attendees = mutableListOf<String>()
    private var duration: String? = null
    private var repeat: Int? = null

    /**
     * 设置提醒动作类型（必需）
     *
     * @param action 提醒动作枚举值
     * - AlarmAction.DISPLAY: 显示提醒
     * - AlarmAction.AUDIO: 音频提醒
     * - AlarmAction.EMAIL: 邮件提醒
     *
     * ```kotlin
     * alarm {
     *     action(AlarmAction.DISPLAY)
     *     // ... 其他配置
     * }
     * ```
     */
    fun action(action: AlarmAction) { this.action = action }

    /**
     * 设置提醒触发时间（必需）
     *
     * 使用 ISO 8601 持续时间格式相对于事件开始时间。
     * 负数表示事件开始前，正数表示事件开始后。
     *
     * @param trigger 触发时间字符串
     * 常用格式：
     * - "-PT15M": 提前15分钟
     * - "-PT1H": 提前1小时
     * - "-P1D": 提前1天
     * - "PT0S": 事件开始时
     *
     * ```kotlin
     * alarm {
     *     trigger("-PT30M")  // 提前30分钟提醒
     *     // ... 其他配置
     * }
     * ```
     */
    fun trigger(trigger: String) { this.trigger = trigger }

    /**
     * 设置提醒触发时间（便捷方法）
     *
     * 使用 Kotlin Duration 对象来设置触发时间，自动转换为 ISO 8601 格式。
     *
     * @param duration 时间间隔 Duration 对象
     * @param beforeEvent 是否在事件开始前，true 表示事件前，false 表示事件后，默认为 true
     *
     * ```kotlin
     * import kotlin.time.Duration.Companion.minutes
     * import kotlin.time.Duration.Companion.hours
     * import kotlin.time.Duration.Companion.days
     *
     * alarm {
     *     // 提前15分钟
     *     trigger(15.minutes)
     *
     *     // 提前1小时
     *     trigger(1.hours)
     *
     *     // 提前1天
     *     trigger(1.days)
     *
     *     // 事件开始时
     *     trigger(Duration.ZERO, beforeEvent = false)
     *
     *     // 事件开始后5分钟
     *     trigger(5.minutes, beforeEvent = false)
     * }
     * ```
     */
    fun trigger(duration: Duration, beforeEvent: Boolean = true) {
        this.trigger = formatDuration(duration, beforeEvent)
    }

    /**
     * 设置提醒描述文本
     *
     * 对于 DISPLAY 和 AUDIO 动作，这是显示给用户的提醒文本。
     *
     * @param description 提醒描述文本
     *
     * ```kotlin
     * alarm {
     *     action(AlarmAction.DISPLAY)
     *     trigger("-PT15M")
     *     description("重要会议即将在15分钟后开始")
     * }
     * ```
     */
    fun description(description: String) { this.description = description }

    /**
     * 设置提醒摘要标题
     *
     * 对于 EMAIL 动作，这通常作为邮件主题。
     * 对于其他动作，作为提醒的标题。
     *
     * @param summary 提醒摘要标题
     *
     * ```kotlin
     * alarm {
     *     action(AlarmAction.EMAIL)
     *     trigger("-P1D")
     *     summary("明天的会议提醒")
     *     description("您明天有一个重要会议")
     * }
     * ```
     */
    fun summary(summary: String) { this.summary = summary }

    /**
     * 添加提醒接收者
     *
     * 主要用于 EMAIL 动作，指定接收提醒邮件的人员。
     * 可以多次调用来添加多个接收者。
     *
     * @param attendee 接收者邮箱地址，格式如："mailto:user@domain.com"
     *
     * ```kotlin
     * alarm {
     *     action(AlarmAction.EMAIL)
     *     trigger("-P1D")
     *     summary("会议提醒")
     *     attendee("mailto:manager@company.com")
     *     attendee("mailto:team@company.com")
     * }
     * ```
     */
    fun attendee(attendee: String) { this.attendees.add(attendee) }

    /**
     * 设置提醒持续时间
     *
     * 指定提醒持续多长时间（主要用于音频提醒）。
     *
     * @param duration ISO 8601 格式的持续时间字符串
     *
     * ```kotlin
     * alarm {
     *     action(AlarmAction.AUDIO)
     *     trigger("-PT5M")
     *     duration("PT30S")  // 播放30秒
     * }
     * ```
     */
    fun duration(duration: String) { this.duration = duration }

    /**
     * 设置提醒重复次数
     *
     * 指定在 duration 间隔内重复提醒多少次。
     *
     * @param repeat 重复次数，必须为正整数
     *
     * ```kotlin
     * alarm {
     *     action(AlarmAction.AUDIO)
     *     trigger("-PT5M")
     *     duration("PT1M")   // 每1分钟重复
     *     repeat(3)          // 重复3次
     * }
     * ```
     */
    fun repeat(repeat: Int) { this.repeat = repeat }

    internal fun build(): IcsAlarm {
        return IcsAlarm(
            action = action,
            trigger = trigger,
            description = description,
            summary = summary,
            attendees = attendees,
            duration = duration,
            repeat = repeat
        )
    }
}
