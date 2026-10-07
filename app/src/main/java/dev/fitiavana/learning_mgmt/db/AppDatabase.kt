package dev.fitiavana.learning_mgmt.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.fitiavana.learning_mgmt.features.backup.BackupDao
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumDao
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.phases.PhaseDao
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatusDao
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.progress.TopicProgressDao
import dev.fitiavana.learning_mgmt.features.topics.Topic
import dev.fitiavana.learning_mgmt.features.topics.TopicDao

/** Bump on any entity change, together with a migration (see `Migrations.kt`). Also stamped on backups. */
const val DB_VERSION = 2

/** Registers the entities and DAOs; each feature package owns its own @Entity and DAO. */
@Database(
    entities = [Curriculum::class, Phase::class, PhaseStatus::class, Topic::class, TopicProgress::class],
    version = DB_VERSION,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
    abstract fun phaseDao(): PhaseDao
    abstract fun phaseStatusDao(): PhaseStatusDao
    abstract fun topicDao(): TopicDao
    abstract fun topicProgressDao(): TopicProgressDao
    abstract fun backupDao(): BackupDao
}
