package top.kagg886.backend.database.converters

import androidx.room3.TypeConverter
import kotlinx.serialization.json.Json
import top.kagg886.backend.database.dao.ExamSyncPayload

class ExamSyncPayloadConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun convertExamPayload(value: String?): ExamSyncPayload? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun reConvertExamPayload(value: ExamSyncPayload?): String? = value?.let { json.encodeToString(it) }
}
