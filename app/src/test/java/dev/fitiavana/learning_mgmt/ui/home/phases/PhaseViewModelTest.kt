package dev.fitiavana.learning_mgmt.ui.home.phases

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhaseViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var curriculumId: String
    private lateinit var first: String
    private lateinit var second: String

    @Before
    fun setUp() = runBlocking {
        curriculumId = env.curricula.create("Spanish")
        first = env.phases.add(curriculumId, "Basics", "## Intro")
        second = env.phases.add(curriculumId, "Verbs", "")
    }

    private fun viewModel(phaseId: String) = PhaseViewModel(phaseId, env.selection, env.progress)

    private suspend fun PhaseViewModel.loaded(predicate: (PhaseViewState.Loaded) -> Boolean = { true }) =
        withTimeout(10_000) {
            uiState.first { it is PhaseViewState.Loaded && predicate(it) } as PhaseViewState.Loaded
        }

    @Test
    fun loadsThePhaseWithItsAction() = runBlocking {
        val state = viewModel(first).loaded()

        assertEquals(
            PhaseWithStatus(Phase(first, curriculumId, 1, "Basics", "## Intro"), Status.NOT_STARTED),
            state.phase,
        )
        assertEquals(PhaseAction.Start, state.action)
    }

    @Test
    fun laterPhasesWaitForTheEarlierOne() = runBlocking {
        val state = viewModel(second).loaded()

        assertEquals(PhaseAction.WaitForStart(Phase(first, curriculumId, 1, "Basics", "## Intro")), state.action)
    }

    @Test
    fun startThenCompleteWalksThroughTheActions() = runBlocking {
        val viewModel = viewModel(first)
        viewModel.loaded()

        viewModel.start()
        assertEquals(PhaseAction.Complete, viewModel.loaded { it.phase.status == Status.IN_PROGRESS }.action)

        viewModel.complete()
        val done = viewModel.loaded { it.phase.status == Status.COMPLETED }
        assertEquals(PhaseAction.None, done.action)
    }

    @Test
    fun otherPhasesWaitForCompletionWhileOneIsInProgress() = runBlocking {
        env.progress.start(first)

        val state = viewModel(second).loaded()

        assertEquals(PhaseAction.WaitForCompletion(Phase(first, curriculumId, 1, "Basics", "## Intro")), state.action)
    }

    @Test
    fun unknownPhaseIsNotFound() = runBlocking {
        val state = withTimeout(10_000) {
            viewModel("missing").uiState.first { it != PhaseViewState.Loading }
        }

        assertEquals(PhaseViewState.NotFound, state)
    }
}
