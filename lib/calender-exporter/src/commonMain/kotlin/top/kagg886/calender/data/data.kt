package top.kagg886.calender.data

import kotlinx.datetime.LocalDateTime

enum class CalenderPermissionGrantType {
    WAIT,
    PROCESSING,

    ALL_GRANTED,
    DENY_ONCE,
    DENY_PERMANENT,

    NOT_SUPPORTED
}

data class Event(
    val id: String,
    val title: String,

    val startTime: LocalDateTime,
    val endTime: LocalDateTime,

    val description: String,
    val location: String,
)