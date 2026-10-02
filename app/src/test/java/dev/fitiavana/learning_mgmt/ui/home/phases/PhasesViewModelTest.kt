package dev.fitiavana.learning_mgmt.ui.home.phases

import dev.fitiavana.learning_mgmt.db.TestEnvironment
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
class PhasesViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var viewModel: PhasesViewModel

    @Before
    fun setUp() {
        viewModel = PhasesViewModel(env.selection, env.progress)
    }

    private suspend fun stateWhere(predicate: (PhasesUiState) -> Boolean) =
        withTimeout(10_000) { viewModel.uiState.first(predicate) }

    @Test
    fun hasNoCurriculumAndNoPhasesAtFirst() = runBlocking {
        val state = stateWhere { !it.loading }

        assertEquals(PhasesUiState(loading = false, curriculumName = null, phases = emptyList()), state)
    }

    @Test
    fun listsThePhasesOfTheSelectedCurriculumWithTheirStatus() = runBlocking {
        val c = env.curricula.create("Spanish")
        val first = env.phases.add(c, "Basics", "")
        env.phases.add(c, "Verbs", "")
        env.progress.start(first)

        val state = stateWhere { s -> s.phases.any { it.status == Status.IN_PROGRESS } }

        assertEquals("Spanish", state.curriculumName)
        assertEquals(
            listOf(1 to Status.IN_PROGRESS, 2 to Status.NOT_STARTED),
            state.phases.map { it.phase.number to it.status },
        )
        assertEquals(listOf("Basics", "Verbs"), state.phases.map { it.phase.name })
    }

    @Test
    fun followsTheSelectedCurriculum() = runBlocking {
        env.curricula.create("Spanish")
        val piano = env.curricula.create("Piano")
        env.phases.add(piano, "Scales", "")

        env.selection.select(piano)

        val state = stateWhere { it.curriculumName == "Piano" && it.phases.isNotEmpty() }
        assertEquals(listOf("Scales"), state.phases.map { it.phase.name })
    }
}
