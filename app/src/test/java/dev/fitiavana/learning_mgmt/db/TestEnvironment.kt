package dev.fitiavana.learning_mgmt.db

import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.testSelectionStore
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

    lateinit var db: AppDatabase
    lateinit var curricula: CurriculumRepository
    lateinit var phases: PhaseRepository
    lateinit var progress: ProgressRepository
    lateinit var selection: CurriculumSelection

    override fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        curricula = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
        phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao())
        selection = CurriculumSelection(
            curricula,
            testSelectionStore(Files.createTempDirectory("selection").toFile(), scope),
        )
    }

    override fun after() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }
}
