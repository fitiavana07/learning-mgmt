package dev.fitiavana.learning_mgmt

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.MIGRATION_1_2
import dev.fitiavana.learning_mgmt.features.topics.TopicRepository
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.SelectedCurriculumStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency wiring: one instance of everything, created once by the Application. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "learning-mgmt.db")
        .addMigrations(MIGRATION_1_2)
        .build()

    val curriculumRepository = CurriculumRepository(database.curriculumDao())
    val phaseRepository = PhaseRepository(database, database.phaseDao())
    val topicRepository = TopicRepository(database, database.topicDao())
    val progressRepository = ProgressRepository(
        database,
        database.phaseDao(),
        database.phaseStatusDao(),
        database.topicProgressDao(),
    )

    private val selectedCurriculumStore = SelectedCurriculumStore(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
            context.preferencesDataStoreFile("selection")
        },
    )

    val curriculumSelection = CurriculumSelection(curriculumRepository, selectedCurriculumStore)
}
