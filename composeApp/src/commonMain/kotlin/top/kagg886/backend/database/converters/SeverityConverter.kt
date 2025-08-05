package top.kagg886.backend.database.converters

import androidx.room.TypeConverter
import co.touchlab.kermit.Severity
import kotlinx.datetime.LocalDateTime

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/5 09:20
 * ================================================
 */

class SeverityConverter {
    @TypeConverter
    fun convertSeverity(value: Severity): Int {
        return value.ordinal
    }

    @TypeConverter
    fun reConvertSeverity(value: Int): Severity {
        return Severity.entries.first { it.ordinal == value }
    }
}
