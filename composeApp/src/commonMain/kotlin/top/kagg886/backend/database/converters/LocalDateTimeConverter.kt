package top.kagg886.backend.database.converters

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime

class LocalDateTimeConverter {
    @TypeConverter
    fun convertDetail(value: LocalDateTime): String {
        return value.toString()
    }

    @TypeConverter
    fun reConvertDetail(value: String): LocalDateTime {
        return LocalDateTime.parse(value)
    }
}
