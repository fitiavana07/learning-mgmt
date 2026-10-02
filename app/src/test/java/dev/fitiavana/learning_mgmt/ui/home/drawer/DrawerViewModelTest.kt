package dev.fitiavana.learning_mgmt.ui.home.drawer

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.testSelectionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DrawerViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var curricula: CurriculumRepository
    private lateinit var phases: PhaseRepository
    private lateinit var progress: ProgressRepository
    private lateinit var viewModel: DrawerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        curricula = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
        phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao())
        viewModel = DrawerViewModel(
            curricula,
            CurriculumSelection(curricula, testSelectionStore(folder.root, scope)),
            progress,
        )
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun itemsWhere(predicate: (List<DrawerItem>) -> Boolean): List<DrawerItem> =
        withTimeout(10_000) { viewModel.items.first(predicate) }

    @Test
    fun isEmptyWithoutCurricula() = runBlocking {
        assertEquals(emptyList<DrawerItem>(), itemsWhere { true })
    }

    @Test
    fun listsCurriculaInCreationOrderWithTheFirstSelected() = runBlocking {
        curricula.create("Spanish")
        curricula.create("Piano")

        val items = itemsWhere { it.size == 2 }

        assertEquals(listOf("Spanish", "Piano"), items.map { it.name })
        assertEquals(listOf(true, false), items.map { it.selected })
    }

    @Test
    fun curriculumWithoutPhasesHasNoPhasesSummary() = runBlocking {
        curricula.create("Spanish")

        val item = itemsWhere { it.isNotEmpty() }.single()

        assertEquals(CurriculumProgress(0, 0, CurriculumProgress.Summary.NoPhases), item.progress)
    }

    @Test
    fun showsProgressOfEachCurriculum() = runBlocking {
        val spanish = curricula.create("Spanish")
        curricula.create("Piano")
        val first = phases.add(spanish, "Basics", "")
        phases.add(spanish, "Verbs", "")
        progress.start(first)

        val spanishItem = itemsWhere { items ->
            items.firstOrNull()?.progress?.summary is CurriculumProgress.Summary.InProgress
        }.first()

        assertEquals(1, spanishItem.progress.position)
        assertEquals(2, spanishItem.progress.total)
    }

    @Test
    fun selectMovesTheSelectionMarker() = runBlocking {
        curricula.create("Spanish")
        val piano = curricula.create("Piano")
        itemsWhere { it.size == 2 }

        viewModel.select(piano)

        assertEquals(
            listOf(false, true),
            itemsWhere { items -> items.find { it.id == piano }?.selected == true }.map { it.selected },
        )
    }
}
