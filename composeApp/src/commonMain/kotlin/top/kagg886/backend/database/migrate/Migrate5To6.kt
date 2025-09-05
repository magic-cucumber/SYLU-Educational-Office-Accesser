package top.kagg886.backend.database.migrate

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import top.kagg886.eoa.util.execute
import top.kagg886.eoa.util.getDoubleByName
import top.kagg886.eoa.util.getIntByName
import top.kagg886.eoa.util.getTextByName
import top.kagg886.eoa.util.query

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/5 11:00
 * ================================================
 */
object Migrate5To6 : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `_new_exams` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `year` TEXT NOT NULL,
                `semester` TEXT NOT NULL,
                `courseID` TEXT NOT NULL,
                `detailsID` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `teacherName` TEXT NOT NULL,
                `credit` REAL NOT NULL,
                `gradePoint` REAL NOT NULL,
                `absoluteScore` TEXT NOT NULL,
                `relateScore` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `degree` INTEGER NOT NULL,
                `detail` TEXT NOT NULL,
                `submitTeacherName` TEXT NOT NULL,
                `submitTime` TEXT NOT NULL
            )
        """.trimIndent()
        )

        with(connection.query("SELECT id, year, semester, courseID, detailsID, name, teacherName, credit, gradePoint, absoluteScore, relateScore, status, degree, detail, submitTime FROM exams")) {
            while (step()) {
                connection.execute(
                    "INSERT INTO `_new_exams` (id, year, semester, courseID, detailsID, name, teacherName, credit, gradePoint, absoluteScore, relateScore, status, degree, detail, submitTeacherName, submitTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    getIntByName("id"),
                    getTextByName("year"),
                    getTextByName("semester"),
                    getTextByName("courseID"),
                    getTextByName("detailsID"),
                    getTextByName("name"),
                    getTextByName("teacherName"),
                    getDoubleByName("credit"),
                    getDoubleByName("gradePoint"),
                    getTextByName("absoluteScore"),
                    getTextByName("relateScore"),
                    getTextByName("status"),
                    getIntByName("degree"),
                    getTextByName("detail"),
                    "同步后查看",
                    getTextByName("submitTime"),
                )
            }
        }

        connection.execSQL("DROP TABLE exams")
        connection.execSQL("ALTER TABLE `_new_exams` RENAME TO exams")


    }
}
