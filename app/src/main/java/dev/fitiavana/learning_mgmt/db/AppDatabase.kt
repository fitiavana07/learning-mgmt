package dev.fitiavana.learning_mgmt.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumDao
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.phases.PhaseDao
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatusDao

/** Registers the entities and DAOs; each feature package owns its own @Entity and DAO. */
@Database(entities = [Curriculum::class, Phase::class, PhaseStatus::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
    abstract fun phaseDao(): PhaseDao
    abstract fun phaseStatusDao(): PhaseStatusDao
}
