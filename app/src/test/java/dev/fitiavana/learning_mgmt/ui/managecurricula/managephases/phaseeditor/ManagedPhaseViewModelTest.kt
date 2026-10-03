package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.home.phases.PhaseViewState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ManagedPhaseViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private fun viewModel(curriculumId: String, phaseId: String) =
        env.track(ManagedPhaseViewModel(curriculumId, phaseId, env.progress))

    private suspend fun ManagedPhaseViewModel.stateWhere(predicate: (PhaseViewState) -> Boolean) =
        withTimeout(10_000) { uiState.first(predicate) }

    @Test
    fun showsThePhaseWithItsStatusAndNoAction() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.phases.add(spanish, "Basics", "# Intro")
        env.progress.start("p1")

        val state = viewModel(spanish, "p1").stateWhere { it is PhaseViewState.Loaded } as PhaseViewState.Loaded

        assertEquals("Basics", state.phase.phase.name)
        assertEquals(Status.IN_PROGRESS, state.phase.status)
        assertEquals(PhaseAction.None, state.action)
    }

    @Test
    fun followsEditsToThePhase() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.phases.add(spanish, "Basics", "")
        val vm = viewModel(spanish, "p1")
        vm.stateWhere { it is PhaseViewState.Loaded }

        env.phases.update("p1", "Foundations", "Text")

        val state = vm.stateWhere { (it as? PhaseViewState.Loaded)?.phase?.phase?.name == "Foundations" }
        assertEquals("Text", (state as PhaseViewState.Loaded).phase.phase.description)
    }

    @Test
    fun aMissingPhaseIsNotFound() = runBlocking {
        val spanish = env.curricula.create("Spanish")

        assertTrue(viewModel(spanish, "nope").stateWhere { it != PhaseViewState.Loading } == PhaseViewState.NotFound)
    }

    @Test
    fun carriesTheTopicsOfThePhase() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.phases.add(spanish, "Basics", "")
        env.topics.add("p1", "Greetings")

        val state = viewModel(spanish, "p1").stateWhere {
            it is PhaseViewState.Loaded && it.topics.isNotEmpty()
        } as PhaseViewState.Loaded

        assertEquals(listOf("Greetings"), state.topics.map { it.topic.name })
    }
}
