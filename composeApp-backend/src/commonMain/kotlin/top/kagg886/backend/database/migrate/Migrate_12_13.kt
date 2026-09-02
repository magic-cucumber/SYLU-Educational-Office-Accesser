package top.kagg886.backend.database.migrate

import androidx.room3.migration.Migration
import androidx.sqlite.async.executeSQL
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.util.getTimeByLessonNumber

private data class MigratedCourseRecord(
    val id: Long,
    val courseId: Long?,
    val startTime: String,
    val endTime: String,
    val isUserAdded: Long,
)

private data class LegacyCourseRecord(
    val id: Long,
    val courseId: Long?,
    val weekNumber: Int,
    val dayOfWeek: Int,
    val periodOfDay: Int,
    val isUserAdded: Long,
)

internal val MIGRATION_12_13 = Migration(12, 13) { connection ->
    val legacyRecords = buildList {
        connection.prepare(
            "SELECT `id`, `courseId`, `weekNumber`, `dayOfWeek`, `periodOfDay`, `isUserAdded` FROM `course_records`"
        ).use { statement ->
            while (statement.step()) {
                add(
                    LegacyCourseRecord(
                        id = statement.getLong(0),
                        courseId = if (statement.isNull(1)) null else statement.getLong(1),
                        weekNumber = statement.getLong(2).toInt(),
                        dayOfWeek = statement.getLong(3).toInt(),
                        periodOfDay = statement.getLong(4).toInt(),
                        isUserAdded = statement.getLong(5),
                    )
                )
            }
        }
    }
    val calender = if (legacyRecords.isEmpty()) null else {
        checkNotNull(AppSyncMMKV.calender) {
            "数据库迁移 12 -> 13 需要校历数据"
        }
    }
    val records = legacyRecords.map { record ->
        val date = checkNotNull(calender).start
            .plus(record.weekNumber - 1, DateTimeUnit.WEEK)
            .plus(record.dayOfWeek - 1, DateTimeUnit.DAY)
        val (startTime, endTime) = getTimeByLessonNumber(record.periodOfDay)

        MigratedCourseRecord(
            id = record.id,
            courseId = record.courseId,
            startTime = date.atTime(startTime).toString(),
            endTime = date.atTime(endTime).toString(),
            isUserAdded = record.isUserAdded,
        )
    }

    connection.executeSQL(
        """
            CREATE TABLE `courses_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `teacherName` TEXT NOT NULL,
                `classroomName` TEXT NOT NULL,
                `credits` REAL NOT NULL,
                `isDegreeRequired` INTEGER NOT NULL,
                `isExaminable` INTEGER NOT NULL,
                `isUserAdded` INTEGER NOT NULL
            )
        """.trimIndent()
    )
    connection.executeSQL(
        """
            INSERT INTO `courses_new` (
                `id`, `name`, `teacherName`, `classroomName`, `credits`,
                `isDegreeRequired`, `isExaminable`, `isUserAdded`
            )
            SELECT
                `id`, `name`, `teacherName`, `classroomName`, `credits`,
                `isDegreeRequired`, `isExaminable`, `isUserAdded`
            FROM `courses`
        """.trimIndent()
    )

    // 先删除子表，避免重建 courses 时触发外键级联删除。
    connection.executeSQL("DROP TABLE `course_records`")
    connection.executeSQL("DROP TABLE `courses`")
    connection.executeSQL("ALTER TABLE `courses_new` RENAME TO `courses`")

    connection.executeSQL(
        """
            CREATE TABLE `course_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `courseId` INTEGER,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT NOT NULL,
                `isUserAdded` INTEGER NOT NULL,
                FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent()
    )
    connection.prepare(
        "INSERT INTO `course_records` (`id`, `courseId`, `startTime`, `endTime`, `isUserAdded`) VALUES (?, ?, ?, ?, ?)"
    ).use { statement ->
        records.forEach { record ->
            statement.bindLong(1, record.id)
            record.courseId?.let { statement.bindLong(2, it) } ?: statement.bindNull(2)
            statement.bindText(3, record.startTime)
            statement.bindText(4, record.endTime)
            statement.bindLong(5, record.isUserAdded)
            statement.step()
            statement.reset()
        }
    }
}
