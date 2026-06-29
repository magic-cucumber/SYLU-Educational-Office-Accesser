package top.kagg886.backend.database.converters

import androidx.room3.TypeConverter
import kotlinx.serialization.json.Json
import top.kagg886.backend.database.dao.GPASyncPayload

class GPASyncPayloadConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun convertGPAPayload(value: String?): GPASyncPayload? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun reConvertGPAPayload(value: GPASyncPayload?): String? = value?.let { json.encodeToString(it) }
}
