package top.kagg886.backend.database.migrate

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import co.touchlab.kermit.Severity
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import top.kagg886.eoa.util.execute
import top.kagg886.eoa.util.getDoubleByName
import top.kagg886.eoa.util.getIntByName
import top.kagg886.eoa.util.getLongByName
import top.kagg886.eoa.util.getTextByName
import top.kagg886.eoa.util.query

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/5 11:03
 * ================================================
 */
object Migrate4To5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        //迁移课程表
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `_new_courses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `teacherName` TEXT NOT NULL,
                `classroomName` TEXT NOT NULL,
                `credits` REAL NOT NULL,
                `isDegreeRequired` INTEGER NOT NULL,
                `isExaminable` INTEGER NOT NULL,
                `yearCode` TEXT NOT NULL,
                `semesterCode` TEXT NOT NULL,
                `isUserAdded` INTEGER NOT NULL
            )
        """.trimIndent()
        )

        with(connection.query("SELECT id, name, teacherName, classroomName, credits, isDegreeRequired, yearCode, semesterCode, isUserAdded FROM courses")) {
            while (step()) {
                connection.execute(
                    "INSERT INTO `_new_courses` (id, name, teacherName, classroomName, credits, isDegreeRequired, isExaminable, yearCode, semesterCode, isUserAdded) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    getLongByName("id"),
                    getTextByName("name"),
                    getTextByName("teacherName"),
                    getTextByName("classroomName"),
                    getDoubleByName("credits"),
                    getIntByName("isDegreeRequired"),
                    0,
                    getTextByName("yearCode"),
                    getTextByName("semesterCode"),
                    getIntByName("isUserAdded")
                )
            }
        }

        connection.execSQL("DROP TABLE courses")
        connection.execSQL("ALTER TABLE `_new_courses` RENAME TO courses")


        connection.execSQL("DROP TABLE log")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `tag` TEXT NOT NULL, `level` INTEGER NOT NULL, `message` TEXT NOT NULL, `time` TEXT NOT NULL, `stacktrace` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `course_extend` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `teacherName` TEXT NOT NULL, `weekNumber` INTEGER NOT NULL, `yearCode` TEXT NOT NULL, `semesterCode` TEXT NOT NULL)")
    }
}
