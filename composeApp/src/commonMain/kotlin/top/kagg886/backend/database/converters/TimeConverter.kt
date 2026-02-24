package top.kagg886.backend.database.converters

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime

class TimeConverter {
    @TypeConverter
    fun convertInstant(value: Instant): Long {
        return value.toEpochMilliseconds()
    }

    @TypeConverter
    fun reConvertInstant(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }

    @TypeConverter
    fun convertLocalDateTime(value: LocalDateTime): String {
        return value.toString()
    }

    @TypeConverter
    fun reConvertLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value)
    }
}
