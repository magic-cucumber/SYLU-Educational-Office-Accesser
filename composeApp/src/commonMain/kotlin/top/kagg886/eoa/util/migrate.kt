package top.kagg886.eoa.util

import androidx.room.util.getColumnIndex
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/5 11:06
 * ================================================
 */

fun SQLiteConnection.query(sql: String, vararg data: Any? = arrayOf()): SQLiteStatement = prepare(sql).apply {
    for ((index, data) in data.withIndex()) {
        if (data == null) {
            bindNull(index + 1)
            continue
        }
        when (data) {
            is Int -> bindInt(index + 1, data)
            is Long -> bindLong(index + 1, data)
            is String -> bindText(index + 1, data)
            is Float -> bindFloat(index + 1, data)
            is Double -> bindDouble(index + 1, data)
            is Boolean -> bindBoolean(index + 1, data)
            is ByteArray -> bindBlob(index + 1, data)
            else -> throw IllegalArgumentException("Unsupported type: ${data::class.simpleName}")
        }
    }
}

fun SQLiteConnection.execute(sql: String, vararg data: Any? = arrayOf()): Boolean = query(sql, data).step()

fun SQLiteStatement.getIntByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getInt(it)
}

fun SQLiteStatement.getLongByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getLong(it)
}

fun SQLiteStatement.getTextByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getText(it)
}


fun SQLiteStatement.getFloatByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getFloat(it)
}

fun SQLiteStatement.getDoubleByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getDouble(it)
}

fun SQLiteStatement.getBooleanByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getBoolean(it)
}

fun SQLiteStatement.getByteArrayByName(name: String) = getColumnIndex(this, name).let {
    if (it == -1) throw IllegalArgumentException("Column not found: $name")
    getBlob(it)
}
