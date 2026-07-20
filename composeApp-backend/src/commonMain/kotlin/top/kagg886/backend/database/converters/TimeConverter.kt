package top.kagg886.backend.database.converters

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime

class TimeConverter {
    @ColumnTypeConverter
    fun convertInstant(value: Instant): Long {
        return value.toEpochMilliseconds()
    }

    @ColumnTypeConverter
    fun reConvertInstant(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }

    @ColumnTypeConverter
    fun convertLocalDateTime(value: LocalDateTime): String {
        return value.toString()
    }

    @ColumnTypeConverter
    fun reConvertLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value)
    }
}
