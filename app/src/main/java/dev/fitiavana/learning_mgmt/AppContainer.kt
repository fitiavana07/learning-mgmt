package dev.fitiavana.learning_mgmt

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.DB_VERSION
import dev.fitiavana.learning_mgmt.db.MIGRATION_1_2
import dev.fitiavana.learning_mgmt.db.MIGRATION_2_3
import dev.fitiavana.learning_mgmt.features.backup.BackupRepository
import dev.fitiavana.learning_mgmt.features.topics.TopicRepository
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.SelectedCurriculumStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.StampClock
import dev.fitiavana.learning_mgmt.features.sync.SyncSettingsStore
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

/** Manual dependency wiring: one instance of everything, created once by the Application. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "learning-mgmt.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val syncSettings = SyncSettingsStore(
        PreferenceDataStoreFactory.create(scope = ioScope) { context.preferencesDataStoreFile("sync") },
    )

    // Every local write is stamped with this device's id, which is needed from the first write on.
    // It is one read of a tiny preferences file, generated on the very first launch only.
    private val changeTracker = ChangeTracker(
        database.syncMetaDao(),
        StampClock(runBlocking { syncSettings.deviceId() }, System::currentTimeMillis),
    )

    val curriculumRepository = CurriculumRepository(database, database.curriculumDao(), changeTracker)
    val phaseRepository = PhaseRepository(database, database.phaseDao(), changeTracker)
    val topicRepository = TopicRepository(database, database.topicDao(), changeTracker)
    val progressRepository = ProgressRepository(
        database,
        database.phaseDao(),
        database.phaseStatusDao(),
        database.topicProgressDao(),
        changeTracker,
    )

    private val selectedCurriculumStore = SelectedCurriculumStore(
        PreferenceDataStoreFactory.create(scope = ioScope) {
            context.preferencesDataStoreFile("selection")
        },
    )

    val curriculumSelection = CurriculumSelection(curriculumRepository, selectedCurriculumStore)

    val backupRepository = BackupRepository(
        database,
        database.backupDao(),
        changeTracker,
        selectedCurriculumStore,
        DB_VERSION,
    )
}
