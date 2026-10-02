package dev.fitiavana.learning_mgmt.ui.home

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.SelectedCurriculumStore
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var curricula: CurriculumRepository
    private lateinit var phases: PhaseRepository
    private lateinit var store: SelectedCurriculumStore
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        curricula = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
        phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        store = testSelectionStore(folder.root, scope)
        viewModel = HomeViewModel(
            CurriculumSelection(curricula, store),
            ProgressRepository(db, db.phaseDao(), db.phaseStatusDao()),
        )
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun phase(id: String, curriculumId: String, number: Int, name: String) =
        Phase(id, curriculumId, number, name, "")

    private suspend fun stateWhere(predicate: (HomeUiState) -> Boolean): HomeUiState =
        withTimeout(10_000) { viewModel.uiState.first(predicate) }

    private suspend fun settled() = stateWhere { it.content != HomeContent.Loading }

    @Test
    fun showsNoCurriculaWhenNoneExist() = runBlocking {
        assertEquals(HomeUiState(null, HomeContent.NoCurricula), settled())
    }

    @Test
    fun showsNoPhasesForAnEmptyCurriculum() = runBlocking {
        curricula.create("Spanish")

        assertEquals(HomeUiState("Spanish", HomeContent.NoPhases), settled())
    }

    @Test
    fun offersToStartTheFirstPhaseWhenNothingStarted() = runBlocking {
        val c = curricula.create("Spanish")
        phases.add(c, "Basics", "")
        phases.add(c, "Verbs", "")

        assertEquals(
            HomeUiState("Spanish", HomeContent.ReadyToStart(phase("p1", c, 1, "Basics"))),
            settled(),
        )
    }

    @Test
    fun startingShowsThePhaseInProgress() = runBlocking {
        val c = curricula.create("Spanish")
        phases.add(c, "Basics", "")
        stateWhere { it.content is HomeContent.ReadyToStart }

        viewModel.startNext()

        assertEquals(
            HomeContent.InProgress(phase("p1", c, 1, "Basics")),
            stateWhere { it.content is HomeContent.InProgress }.content,
        )
    }

    @Test
    fun completingOffersTheNextPhaseThenFinishes() = runBlocking {
        val c = curricula.create("Spanish")
        phases.add(c, "Basics", "")
        phases.add(c, "Verbs", "")
        stateWhere { it.content is HomeContent.ReadyToStart }
        viewModel.startNext()
        stateWhere { it.content is HomeContent.InProgress }

        viewModel.completeCurrent()
        val second = stateWhere { it.content is HomeContent.ReadyToStart }
        assertEquals(HomeContent.ReadyToStart(phase("p2", c, 2, "Verbs")), second.content)

        viewModel.startNext()
        stateWhere { it.content is HomeContent.InProgress }
        viewModel.completeCurrent()
        assertEquals(HomeContent.AllCompleted, stateWhere { it.content == HomeContent.AllCompleted }.content)
    }

    @Test
    fun opensTheLastSelectedCurriculum() = runBlocking {
        curricula.create("Spanish")
        val piano = curricula.create("Piano")
        store.select(piano)

        assertEquals("Piano", stateWhere { it.curriculumName == "Piano" }.curriculumName)
    }

    @Test
    fun fallsBackToTheFirstCurriculumWhenTheSelectedOneIsGone() = runBlocking {
        curricula.create("Spanish")
        store.select("deleted-id")

        assertEquals("Spanish", settled().curriculumName)
    }

    @Test
    fun actionsWithoutAMatchingPhaseDoNothing() = runBlocking {
        curricula.create("Spanish")
        settled()

        viewModel.startNext()
        viewModel.completeCurrent()

        assertTrue(settled().content == HomeContent.NoPhases)
    }
}
