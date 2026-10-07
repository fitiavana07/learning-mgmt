package dev.fitiavana.learning_mgmt.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.backup.BackupRepository
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.testSelectionStore
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
class TestEnvironment : ExternalResource() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val viewModels = mutableListOf<ViewModel>()

    lateinit var db: AppDatabase
    lateinit var curricula: CurriculumRepository
    lateinit var phases: PhaseRepository
    lateinit var topics: TopicRepository
    lateinit var progress: ProgressRepository
    lateinit var selection: CurriculumSelection
    lateinit var backup: BackupRepository

    override fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        curricula = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
        phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        topics = TopicRepository(db, db.topicDao(), sequentialIds("t"))
        progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao(), db.topicProgressDao())
        val selectionStore = testSelectionStore(Files.createTempDirectory("selection").toFile(), scope)
        selection = CurriculumSelection(curricula, selectionStore)
        backup = BackupRepository(db, db.backupDao(), selectionStore, DB_VERSION)
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
