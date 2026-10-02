package dev.fitiavana.learning_mgmt.ui.home

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.phases.Phase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = env.track(HomeViewModel(env.selection, env.progress))
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
        env.curricula.create("Spanish")

        assertEquals(HomeUiState("Spanish", HomeContent.NoPhases), settled())
    }

    @Test
    fun offersToStartTheFirstPhaseWhenNothingStarted() = runBlocking {
        val c = env.curricula.create("Spanish")
        env.phases.add(c, "Basics", "")
        env.phases.add(c, "Verbs", "")

        assertEquals(
            HomeUiState("Spanish", HomeContent.ReadyToStart(phase("p1", c, 1, "Basics"))),
            settled(),
        )
    }

    @Test
    fun startingShowsThePhaseInProgress() = runBlocking {
        val c = env.curricula.create("Spanish")
        env.phases.add(c, "Basics", "")
        stateWhere { it.content is HomeContent.ReadyToStart }

        viewModel.startNext()

        assertEquals(
            HomeContent.InProgress(phase("p1", c, 1, "Basics")),
            stateWhere { it.content is HomeContent.InProgress }.content,
        )
    }

    @Test
    fun completingOffersTheNextPhaseThenFinishes() = runBlocking {
        val c = env.curricula.create("Spanish")
        env.phases.add(c, "Basics", "")
        env.phases.add(c, "Verbs", "")
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
        env.curricula.create("Spanish")
        val piano = env.curricula.create("Piano")
        env.selection.select(piano)

        assertEquals("Piano", stateWhere { it.curriculumName == "Piano" }.curriculumName)
    }

    @Test
    fun fallsBackToTheFirstCurriculumWhenTheSelectedOneIsGone() = runBlocking {
        env.curricula.create("Spanish")
        env.selection.select("deleted-id")

        assertEquals("Spanish", settled().curriculumName)
    }

    @Test
    fun actionsWithoutAMatchingPhaseDoNothing() = runBlocking {
        env.curricula.create("Spanish")
        settled()

        viewModel.startNext()
        viewModel.completeCurrent()

        assertTrue(settled().content == HomeContent.NoPhases)
    }
}
