import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import top.kagg886.ics.IcsBuilder
import top.kagg886.ics.data.AlarmAction
import top.kagg886.ics.data.Frequency
import top.kagg886.ics.ics
import kotlin.time.Duration.Companion.minutes

class IcsCompatibilityTest {
    @Test
    fun fixedOffsetEventIsSerializedAsUtcWithoutTzid() {
        val content = buildIcs {
            event {
                uid("course-20260817@example.com")
                timeZone(TimeZone.of("GMT+08:00"))
                startTime(LocalDateTime(2026, 8, 17, 8, 0))
                endTime(LocalDateTime(2026, 8, 17, 8, 45))
            }
        }

        assertContains(content, "DTSTART:20260817T000000Z\r\n")
        assertContains(content, "DTEND:20260817T004500Z\r\n")
        assertFalse(content.contains("TZID="))
        assertFalse(content.replace("\r\n", "").contains("\n"))
    }

    @Test
    fun recurrenceUntilUsesTheSameUtcDateTimeForm() {
        val content = buildIcs {
            event {
                timeZone(TimeZone.of("GMT+08:00"))
                startTime(LocalDateTime(2026, 8, 17, 8, 0))
                recurrence {
                    frequency(Frequency.DAILY)
                    until(LocalDateTime(2026, 8, 20, 8, 0))
                }
            }
        }

        assertContains(content, "RRULE:FREQ=DAILY;UNTIL=20260820T000000Z\r\n")
    }

    @Test
    fun alarmAndTextEscapingRemainValidIcsProperties() {
        val content = buildIcs {
            event {
                startTime(LocalDateTime(2026, 8, 17, 8, 0))
                description("教师: 张教授\n第二行,;\\")
                alarm {
                    action(AlarmAction.AUDIO)
                    trigger(30.minutes)
                    description("课程 即将开始")
                }
            }
        }

        assertContains(content, "DESCRIPTION:教师: 张教授\\n第二行\\,\\;\\\\\r\n")
        assertContains(content, "ACTION:AUDIO\r\n")
        assertContains(content, "TRIGGER:-PT30M\r\n")
    }

    private fun buildIcs(block: IcsBuilder.() -> Unit): String {
        var content: String? = null
        ics {
            writeTo { content = it }
            block()
        }
        return checkNotNull(content)
    }
}
