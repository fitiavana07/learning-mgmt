package dev.fitiavana.learning_mgmt.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.backup.BackupRepository
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.testSelectionStore
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.SyncRepository
import dev.fitiavana.learning_mgmt.features.topics.TopicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.ExternalResource
import java.nio.file.Files

/**
 * Real in-memory database, repositories and selection store for Robolectric tests, with a test
 * main dispatcher so ViewModels run eagerly. Ids are predictable: c1, c2... / p1, p2...
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestEnvironment(
    private val deviceId: String = "test",
    /** Prefix of the generated ids, so two environments acting as two devices never reuse an id. */
    private val idPrefix: String = "",
) : ExternalResource() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val viewModels = mutableListOf<ViewModel>()

    /** The wall clock of the tracker; stamps still rise when it stands still. */
    var now = 1_000L

    lateinit var db: AppDatabase
    lateinit var tracker: ChangeTracker
    lateinit var curricula: CurriculumRepository
    lateinit var phases: PhaseRepository
    lateinit var topics: TopicRepository
    lateinit var progress: ProgressRepository
    lateinit var selection: CurriculumSelection
    lateinit var backup: BackupRepository
    lateinit var sync: SyncRepository

    override fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        tracker = testTracker(db, deviceId) { now }
        curricula = CurriculumRepository(db, db.curriculumDao(), tracker, sequentialIds("${idPrefix}c"))
        phases = PhaseRepository(db, db.phaseDao(), tracker, sequentialIds("${idPrefix}p"))
        topics = TopicRepository(db, db.topicDao(), tracker, sequentialIds("${idPrefix}t"))
        progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao(), db.topicProgressDao(), tracker)
        val selectionStore = testSelectionStore(Files.createTempDirectory("selection").toFile(), scope)
        selection = CurriculumSelection(curricula, selectionStore)
        backup = BackupRepository(db, db.backupDao(), tracker, selectionStore, DB_VERSION)
        sync = SyncRepository(db, db.backupDao(), db.syncDao(), db.syncMetaDao(), tracker, DB_VERSION)
    }

    /** Registers a ViewModel so its coroutines are cancelled when the test ends. */
    fun <T : ViewModel> track(viewModel: T): T = viewModel.also { viewModels += it }

    override fun after() {
        viewModels.forEach { it.viewModelScope.cancel() }
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }
}
