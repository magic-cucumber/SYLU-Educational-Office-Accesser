package top.kagg886.ics

/**
 * 创建 ICS 日历的主入口函数
 * 
 * 使用 DSL 语法创建符合 RFC 5545 标准的 iCalendar 格式日历文件。
 * 支持添加多个事件，每个事件可以包含详细信息、重复规则、提醒等。
 * 
 * @param block IcsBuilder 的 DSL 配置块，用于配置日历和添加事件
 * 
 * @sample
 * ```kotlin
 * ics {
 *     writeTo { icsContent -> 
 *         println(icsContent)
 *         // 或者保存到文件
 *         File("my-calendar.ics").writeText(icsContent)
 *     }
 *     
 *     // 设置日历元数据（可选）
 *     prodId("-//MyCompany//MyApp//EN")
 *     method("PUBLISH")
 *     
 *     // 添加简单事件
 *     event {
 *         summary("团队会议")
 *         description("讨论项目进度")
 *         location("会议室A")
 *         startTime(LocalDateTime(2024, 1, 15, 14, 0))
 *         endTime(LocalDateTime(2024, 1, 15, 16, 0))
 *         
 *         alarm {
 *             action(AlarmAction.DISPLAY)
 *             trigger("-PT15M")
 *             description("会议即将开始")
 *         }
 *     }
 *     
 *     // 添加重复事件
 *     event {
 *         summary("每周例会")
 *         startTime(LocalDateTime(2024, 1, 8, 10, 0))
 *         endTime(LocalDateTime(2024, 1, 8, 11, 0))
 *         
 *         recurrence {
 *             frequency(Frequency.WEEKLY)
 *             count(10)
 *             byDay("MO")
 *         }
 *     }
 * }
 * ```
 * 
 * @see IcsBuilder 日历构建器
 * @see EventBuilder 事件构建器
 * @see RecurrenceBuilder 重复规则构建器
 * @see AlarmBuilder 提醒构建器
 */
fun ics(block: IcsBuilder.() -> Unit) = IcsBuilder().apply(block).build()