package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.progress.Status
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ManagePhasesViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private fun viewModel(curriculumId: String) =
        env.track(ManagePhasesViewModel(curriculumId, env.curricula, env.progress, env.phases))

    private suspend fun ManagePhasesViewModel.stateWhere(predicate: (ManagePhasesState) -> Boolean) =
        withTimeout(10_000) { uiState.first(predicate) }

    private suspend fun spanishWithThreePhases(): String {
        val spanish = env.curricula.create("Spanish")
        env.phases.add(spanish, "Basics", "")
        env.phases.add(spanish, "Verbs", "")
        env.phases.add(spanish, "Tenses", "")
        return spanish
    }

    @Test
    fun showsTheCurriculumNameAndItsPhasesInOrder() = runBlocking {
        val vm = viewModel(spanishWithThreePhases())

        val state = vm.stateWhere { it.rows.size == 3 }

        assertEquals("Spanish", state.curriculumName)
        assertEquals(
            listOf(1 to "Basics", 2 to "Verbs", 3 to "Tenses"),
            state.rows.map { it.number to it.name },
        )
    }

    @Test
    fun onlyShowsThePhasesOfItsOwnCurriculum() = runBlocking {
        val spanish = spanishWithThreePhases()
        val piano = env.curricula.create("Piano")
        env.phases.add(piano, "Scales", "")

        val state = viewModel(piano).stateWhere { it.rows.isNotEmpty() }

        assertEquals(listOf("Scales"), state.rows.map { it.name })
        assertEquals(3, viewModel(spanish).stateWhere { it.rows.isNotEmpty() }.rows.size)
    }

    @Test
    fun rowsCarryTheirStatus() = runBlocking {
        val spanish = spanishWithThreePhases()
        env.progress.start("p1")

        val state = viewModel(spanish).stateWhere { it.rows.firstOrNull()?.status == Status.IN_PROGRESS }

        assertEquals(
            listOf(Status.IN_PROGRESS, Status.NOT_STARTED, Status.NOT_STARTED),
            state.rows.map { it.status },
        )
    }

    @Test
    fun deleteRemovesThePhaseAndRenumbers() = runBlocking {
        val vm = viewModel(spanishWithThreePhases())
        vm.stateWhere { it.rows.size == 3 }

        vm.delete("p2")

        val state = vm.stateWhere { it.rows.size == 2 }
        assertEquals(listOf(1 to "Basics", 2 to "Tenses"), state.rows.map { it.number to it.name })
    }

    @Test
    fun moveReordersAndRenumbers() = runBlocking {
        val vm = viewModel(spanishWithThreePhases())
        vm.stateWhere { it.rows.size == 3 }

        vm.move(from = 0, to = 2)

        val state = vm.stateWhere { it.rows.firstOrNull()?.name == "Verbs" }
        assertEquals(
            listOf(1 to "Verbs", 2 to "Tenses", 3 to "Basics"),
            state.rows.map { it.number to it.name },
        )
    }

    @Test
    fun aPhaseKeepsItsStatusWhenMoved() = runBlocking {
        val spanish = spanishWithThreePhases()
        env.progress.start("p1")
        val vm = viewModel(spanish)
        vm.stateWhere { it.rows.size == 3 }

        vm.move(from = 0, to = 2)

        val state = vm.stateWhere { it.rows.firstOrNull()?.name == "Verbs" }
        assertEquals(Status.IN_PROGRESS, state.rows.last().status)
        assertEquals("Basics", state.rows.last().name)
    }
}
