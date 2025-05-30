package top.kagg886.backend.database.converters

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class ExamConverter {
    @TypeConverter
    fun convertDetail(value: String): List<List<String>> = Json.decodeFromString(value)

    @TypeConverter
    fun reConvertDetail(value: List<List<String>>): String = Json.encodeToString(value)
}