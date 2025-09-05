package top.kagg886.backend.database.migrate

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/5 10:41
 * ================================================
 */
object Migrate6To7 : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE log")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `tag` TEXT NOT NULL,
                `level` INTEGER NOT NULL,
                `message` TEXT NOT NULL,
                `time` INTEGER NOT NULL,
                `stacktrace` TEXT
            )
        """.trimIndent())
//
//
//        val cursor = connection.query("SELECT id, tag, level, message, time, stacktrace FROM log")
//
//        while (cursor.step()) {
//            connection.execute(
//                "INSERT INTO `_new_log` (id, tag, level, message, time, stacktrace) VALUES (?, ?, ?, ?, ?, ?)",
//                cursor.getLongByName("id"),
//                cursor.getTextByName("tag"),
//                cursor.getIntByName("level"),
//                cursor.getTextByName("message"),
//                LocalDateTime.parse(cursor.getTextByName("time")).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
//                cursor.getTextByName("stacktrace")
//            )
//        }
//
//        connection.execSQL("DROP TABLE log")
//        connection.execSQL("ALTER TABLE `_new_log` RENAME TO log")
    }
}
