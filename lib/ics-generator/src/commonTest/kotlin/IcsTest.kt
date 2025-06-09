import top.kagg886.ics.ics
import top.kagg886.ics.data.*
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlin.test.Test

class IcsTest {
    @Test
    fun testIcsWrite() {
        ics {
            writeTo {
                println("生成的ICS内容:")
                println(it)
            }

            // 添加一个简单的事件
            event {
                summary("团队会议")
                description("讨论项目进度和下一步计划")
                location("会议室A")
                startTime(LocalDateTime(2024, 1, 15, 10, 0))
                endTime(LocalDateTime(2024, 1, 15, 11, 30))
                organizer("mailto:manager@company.com")
                attendee("mailto:john@company.com")
                attendee("mailto:jane@company.com")
                status(EventStatus.CONFIRMED)
                priority(1)
                category("工作")

                // 添加提醒：提前15分钟 (使用 Duration API)
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(15.minutes) // 使用 kotlinx.datetime Duration API
                    description("会议将在15分钟后开始")
                }
            }

            // 添加一个全天事件
            event {
                summary("公司年会")
                description("年度总结大会")
                location("大礼堂")
                startTime(LocalDateTime(2024, 2, 1, 0, 0))
                endTime(LocalDateTime(2024, 2, 2, 0, 0))
                allDay(true)
                status(EventStatus.CONFIRMED)
                category("公司活动")
                classification(Classification.PUBLIC)
            }

            // 添加重复事件
            event {
                summary("每周例会")
                description("团队周例会")
                location("会议室B")
                startTime(LocalDateTime(2024, 1, 8, 14, 0))
                endTime(LocalDateTime(2024, 1, 8, 15, 0))
                status(EventStatus.CONFIRMED)

                // 每周重复，持续10次
                recurrence {
                    frequency(Frequency.WEEKLY)
                    count(10)
                    byDay("MO") // 每周一
                }

                // 提前5分钟提醒 (使用 Duration API)
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(5.minutes) // 使用 kotlinx.datetime Duration API
                    description("例会提醒")
                }
            }

            // 添加生日提醒（每年重复）
            event {
                summary("张三的生日")
                startTime(LocalDateTime(2024, 3, 15, 0, 0))
                allDay(true)

                // 每年重复
                recurrence {
                    frequency(Frequency.YEARLY)
                }

                                // 当天提醒 (使用 Duration API)
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(kotlin.time.Duration.ZERO, beforeEvent = false) // 事件开始时
                    description("今天是张三的生日！")
                }
                
                // 提前一天提醒 (使用 Duration API)
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(1.days) // 使用 kotlinx.datetime Duration API
                    description("明天是张三的生日，记得准备礼物")
                }
            }
        }
    }

    @Test
    fun testComplexEvent() {
        ics {
            writeTo { icsContent ->
                println("生成的ICS内容:")
                println(icsContent)
            }

            // 复杂事件示例
            event {
                uid("complex-event-2024@example.com")
                summary("项目启动会")
                description("新项目启动会议\n\n议程:\n1. 项目介绍\n2. 团队分工\n3. 时间安排\n4. Q&A")
                location("北京市朝阳区xx大厦22层会议室")
                startTime(LocalDateTime(2024, 1, 10, 9, 0))
                endTime(LocalDateTime(2024, 1, 10, 12, 0))

                organizer("CN=项目经理:mailto:pm@company.com")
                attendee("CN=开发工程师:mailto:dev1@company.com")
                attendee("CN=测试工程师:mailto:qa1@company.com")
                attendee("CN=产品经理:mailto:product@company.com")

                status(EventStatus.CONFIRMED)
                priority(1)
                url("https://company.com/projects/meeting-notes")

                category("项目管理")
                category("重要会议")

                transparency(Transparency.OPAQUE)
                classification(Classification.CONFIDENTIAL)

                // 多个提醒
                alarm {
                    action(AlarmAction.EMAIL)
                    trigger("-P1D")
                    summary("明天的项目启动会")
                    description("提醒：明天上午9点有项目启动会")
                    attendee("mailto:pm@company.com")
                }

                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger("-PT30M")
                    description("会议将在30分钟后开始")
                }
                
                alarm {
                    action(AlarmAction.AUDIO)
                    trigger("-PT5M")
                    description("会议即将开始")
                }
            }
        }
    }
    
    @Test
    fun testDurationAPI() {
        ics {
            writeTo { icsContent ->
                println("Duration API 示例:")
                println(icsContent)
            }
            
            // 使用 Duration API 的示例
            event {
                summary("Duration API 测试")
                description("展示如何使用 kotlinx.datetime Duration API")
                startTime(LocalDateTime(2024, 1, 20, 10, 0))
                
                // 使用 Duration API 设置事件持续时间
                duration(2.hours) // 持续2小时，等价于 "PT2H"
                
                status(EventStatus.CONFIRMED)
                
                // 多种 Duration API 用法的提醒
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(15.minutes) // 提前15分钟，等价于 "-PT15M"
                    description("15分钟后开始")
                }
                
                alarm {
                    action(AlarmAction.EMAIL)
                    trigger(1.hours) // 提前1小时，等价于 "-PT1H"  
                    summary("1小时后有会议")
                    attendee("mailto:user@company.com")
                }
                
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(1.days) // 提前1天，等价于 "-P1D"
                    description("明天有重要会议")
                }
                
                alarm {
                    action(AlarmAction.AUDIO)
                    trigger(kotlin.time.Duration.ZERO, beforeEvent = false) // 事件开始时，等价于 "PT0S"
                    description("会议开始了")
                }
                
                alarm {
                    action(AlarmAction.DISPLAY)
                    trigger(5.minutes, beforeEvent = false) // 事件开始后5分钟，等价于 "PT5M"
                    description("会议已经开始5分钟了")
                }
            }
        }
    }
}