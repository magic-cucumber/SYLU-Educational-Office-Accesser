package top.kagg886.backend.database.converters

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

class ExamConverter {
    @ColumnTypeConverter
    fun convertDetail(value: String): List<List<String>> = Json.decodeFromString(value)

    @ColumnTypeConverter
    fun reConvertDetail(value: List<List<String>>): String = Json.encodeToString(value)
}
