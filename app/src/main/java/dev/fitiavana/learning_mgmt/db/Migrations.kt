package dev.fitiavana.learning_mgmt.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the curriculum order and the sync metadata. Curricula are numbered in insertion order (what
 * they were ordered by so far) and every existing row gets a zero stamp, older than any real write.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `curriculum` ADD COLUMN `number` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE `curriculum` SET `number` = " +
                "(SELECT COUNT(*) FROM `curriculum` AS other WHERE other.rowid <= `curriculum`.rowid)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_meta` (`kind` TEXT NOT NULL, `id` TEXT NOT NULL, " +
                "`stampTime` INTEGER NOT NULL, `stampDevice` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
                "PRIMARY KEY(`kind`, `id`))",
        )
        listOf(
            "CURRICULUM" to ("curriculum" to "id"),
            "PHASE" to ("phase" to "id"),
            "TOPIC" to ("topic" to "id"),
            "PHASE_STATUS" to ("phase_status" to "phaseId"),
            "TOPIC_PROGRESS" to ("topic_progress" to "topicId"),
        ).forEach { (kind, source) ->
            val (table, idColumn) = source
            db.execSQL(
                "INSERT INTO `sync_meta` (`kind`, `id`, `stampTime`, `stampDevice`, `deleted`) " +
                    "SELECT '$kind', `$idColumn`, 0, '', 0 FROM `$table`",
            )
        }
    }
}

/** Adds the topic structure and topic progress tables. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `topic` (`id` TEXT NOT NULL, `phaseId` TEXT NOT NULL, " +
                "`number` INTEGER NOT NULL, `name` TEXT NOT NULL, `total` INTEGER, `unit` TEXT, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`phaseId`) REFERENCES `phase`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_topic_phaseId` ON `topic` (`phaseId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `topic_progress` (`topicId` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`done` INTEGER NOT NULL, PRIMARY KEY(`topicId`), FOREIGN KEY(`topicId`) REFERENCES `topic`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }
}
