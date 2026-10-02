package dev.fitiavana.learning_mgmt.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumDao

/** Registers the entities and DAOs; each feature package owns its own @Entity and DAO. */
@Database(entities = [Curriculum::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao
}
