package top.kagg886.backend.database.migrate

import androidx.room3.migration.Migration
import androidx.sqlite.async.executeSQL

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/29 11:22
 * ================================================
 */

internal val MIGRATION_10_11 = Migration(10, 11) { connection ->
    connection.executeSQL(
        """
            CREATE TABLE IF NOT EXISTS `sync-overviews` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `updatedStamp` INTEGER NOT NULL,
                `success` INTEGER NOT NULL DEFAULT true
            )
        """.trimIndent()
    )
    connection.executeSQL(
        """
            CREATE TABLE IF NOT EXISTS `sync-checkpoints` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `overviewId` INTEGER NOT NULL,
                `updatedStamp` INTEGER NOT NULL,
                `profileSuccess` INTEGER NOT NULL,
                `calendarSuccess` INTEGER NOT NULL,
                `examSuccess` INTEGER NOT NULL,
                `gpaSuccess` INTEGER NOT NULL,
                `noticeSuccess` INTEGER NOT NULL,
                `termSuccess` INTEGER NOT NULL,
                `courseSuccess` INTEGER NOT NULL,
                `examPayload` TEXT,
                `gpaPayload` TEXT,
                FOREIGN KEY(`overviewId`) REFERENCES `sync-overviews`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent()
    )
    connection.executeSQL(
        """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_sync-checkpoints_overviewId`
            ON `sync-checkpoints` (`overviewId`)
        """.trimIndent()
    )
    connection.executeSQL(
        """
            INSERT INTO `sync-overviews` (`id`, `updatedStamp`, `success`)
            SELECT `id`, `updatedStamp`, `success`
            FROM `sync_records`
            ORDER BY `id` DESC
            LIMIT 1
        """.trimIndent()
    )
    connection.executeSQL(
        """
            INSERT INTO `sync-checkpoints` (
                `overviewId`,
                `updatedStamp`,
                `profileSuccess`,
                `calendarSuccess`,
                `examSuccess`,
                `gpaSuccess`,
                `noticeSuccess`,
                `termSuccess`,
                `courseSuccess`
            )
            SELECT
                `id`,
                `updatedStamp`,
                `success`,
                `success`,
                `success`,
                `success`,
                `success`,
                `success`,
                `success`
            FROM `sync_records`
            ORDER BY `id` DESC
            LIMIT 1
        """.trimIndent()
    )
    connection.executeSQL("DROP TABLE `sync_records`")
}
