package top.kagg886.calender.data

enum class CalenderPermissionGrantType {
    WAIT,
    PROCESSING,

    ALL_GRANTED,
    DENY_ONCE,
    DENY_PERMANENT,

    NOT_SUPPORTED
}

data class Event(
    val title: String,
    val start: Long,
    val end: Long
)