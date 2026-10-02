package dev.fitiavana.learning_mgmt.ui.home.drawer

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress
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
class DrawerViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var viewModel: DrawerViewModel

    @Before
    fun setUp() {
        viewModel = env.track(DrawerViewModel(env.curricula, env.selection, env.progress))
    }

    private suspend fun itemsWhere(predicate: (List<DrawerItem>) -> Boolean): List<DrawerItem> =
        withTimeout(10_000) { viewModel.items.first(predicate) }

    @Test
    fun isEmptyWithoutCurricula() = runBlocking {
        assertEquals(emptyList<DrawerItem>(), itemsWhere { true })
    }

    @Test
    fun listsCurriculaInCreationOrderWithTheFirstSelected() = runBlocking {
        env.curricula.create("Spanish")
        env.curricula.create("Piano")

        val items = itemsWhere { it.size == 2 }

        assertEquals(listOf("Spanish", "Piano"), items.map { it.name })
        assertEquals(listOf(true, false), items.map { it.selected })
    }

    @Test
    fun curriculumWithoutPhasesHasNoPhasesSummary() = runBlocking {
        env.curricula.create("Spanish")

        val item = itemsWhere { it.isNotEmpty() }.single()

        assertEquals(CurriculumProgress(0, 0, CurriculumProgress.Summary.NoPhases), item.progress)
    }

    @Test
    fun showsProgressOfEachCurriculum() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.curricula.create("Piano")
        val first = env.phases.add(spanish, "Basics", "")
        env.phases.add(spanish, "Verbs", "")
        env.progress.start(first)

        val spanishItem = itemsWhere { items ->
            items.firstOrNull()?.progress?.summary is CurriculumProgress.Summary.InProgress
        }.first()

        assertEquals(1, spanishItem.progress.position)
        assertEquals(2, spanishItem.progress.total)
    }

    @Test
    fun selectMovesTheSelectionMarker() = runBlocking {
        env.curricula.create("Spanish")
        val piano = env.curricula.create("Piano")
        itemsWhere { it.size == 2 }

        viewModel.select(piano)

        assertEquals(
            listOf(false, true),
            itemsWhere { items -> items.find { it.id == piano }?.selected == true }.map { it.selected },
        )
    }
}
