package dev.fitiavana.learning_mgmt.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
