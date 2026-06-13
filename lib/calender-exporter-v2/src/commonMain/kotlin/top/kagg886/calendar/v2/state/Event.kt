package top.kagg886.calendar.v2.state

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String? = null,
    val title: String,

    val startTime: LocalDateTime,
    val endTime: LocalDateTime,

    val description: String,
    val location: String,
)
