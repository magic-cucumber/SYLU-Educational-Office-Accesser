package top.kagg886.backend.database.converters

import androidx.room3.ColumnTypeConverter
import co.touchlab.kermit.Severity
import kotlinx.datetime.LocalDateTime

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/5 09:20
 * ================================================
 */

class SeverityConverter {
    @ColumnTypeConverter
    fun convertSeverity(value: Severity): Int {
        return value.ordinal
    }

    @ColumnTypeConverter
    fun reConvertSeverity(value: Int): Severity {
        return Severity.entries.first { it.ordinal == value }
    }
}
