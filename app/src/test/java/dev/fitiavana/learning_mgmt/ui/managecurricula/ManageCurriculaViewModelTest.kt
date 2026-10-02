package dev.fitiavana.learning_mgmt.ui.managecurricula

import dev.fitiavana.learning_mgmt.db.TestEnvironment
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
class ManageCurriculaViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var viewModel: ManageCurriculaViewModel

    @Before
    fun setUp() {
        viewModel = env.track(ManageCurriculaViewModel(env.curricula, env.progress))
    }

    private suspend fun rowsWhere(predicate: (List<CurriculumRow>) -> Boolean): List<CurriculumRow> =
        withTimeout(10_000) { viewModel.rows.first(predicate) }

    @Test
    fun isEmptyWithoutCurricula() = runBlocking {
        assertEquals(emptyList<CurriculumRow>(), rowsWhere { true })
    }

    @Test
    fun listsCurriculaWithTheirPhaseCount() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.curricula.create("Piano")
        env.phases.add(spanish, "Basics", "")
        env.phases.add(spanish, "Verbs", "")

        val rows = rowsWhere { it.size == 2 && it.first().phaseCount == 2 }

        assertEquals(listOf("Spanish" to 2, "Piano" to 0), rows.map { it.name to it.phaseCount })
    }

    @Test
    fun createAddsACurriculum() = runBlocking {
        viewModel.create("  Spanish ")

        assertEquals(listOf("Spanish"), rowsWhere { it.isNotEmpty() }.map { it.name })
    }

    @Test
    fun renameChangesTheName() = runBlocking {
        val id = env.curricula.create("Spanish")
        rowsWhere { it.isNotEmpty() }

        viewModel.rename(id, "Español")

        assertEquals(listOf("Español"), rowsWhere { it.singleOrNull()?.name == "Español" }.map { it.name })
    }

    @Test
    fun deleteRemovesTheCurriculumAndItsPhases() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.curricula.create("Piano")
        env.phases.add(spanish, "Basics", "")
        rowsWhere { it.size == 2 }

        viewModel.delete(spanish)

        assertEquals(listOf("Piano"), rowsWhere { it.size == 1 }.map { it.name })
        assertEquals(emptyList<Any>(), env.phases.observe(spanish).first())
    }
}
