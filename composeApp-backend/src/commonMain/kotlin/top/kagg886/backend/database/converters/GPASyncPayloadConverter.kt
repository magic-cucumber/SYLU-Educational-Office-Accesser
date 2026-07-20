package top.kagg886.backend.database.converters

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json
import top.kagg886.backend.database.dao.GPASyncPayload

class GPASyncPayloadConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @ColumnTypeConverter
    fun convertGPAPayload(value: String?): GPASyncPayload? = value?.let { json.decodeFromString(it) }

    @ColumnTypeConverter
    fun reConvertGPAPayload(value: GPASyncPayload?): String? = value?.let { json.encodeToString(it) }
}
