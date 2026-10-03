package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.progress.Status
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ManageTopicsViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var curriculumId: String

    private suspend fun phaseWithTopics(): String {
        curriculumId = env.curricula.create("Spanish")
        val phase = env.phases.add(curriculumId, "Basics", "")
        env.topics.add(phase, "Greetings")
        env.topics.add(phase, "Chapter 1", total = 40, unit = "pages")
        return phase
    }

    private fun vm(phaseId: String) =
        env.track(ManageTopicsViewModel(curriculumId, phaseId, env.progress, env.topics))

    private suspend fun ManageTopicsViewModel.loaded() =
        withTimeout(10_000) { uiState.first { it.phaseName.isNotEmpty() } }

    @Test
    fun showsThePhaseAndItsTopicsWithTheirProgress() = runBlocking {
        val state = vm(phaseWithTopics()).loaded()

        assertEquals(1, state.phaseNumber)
        assertEquals("Basics", state.phaseName)
        assertEquals(
            listOf(
                TopicRow("t1", 1, "Greetings", Status.NOT_STARTED, total = null, done = 0, unit = null),
                TopicRow("t2", 2, "Chapter 1", Status.NOT_STARTED, total = 40, done = 0, unit = "pages"),
            ),
            state.rows,
        )
        assertFalse(state.locked)
    }

    @Test
    fun aCompletedPhaseLocksItsTopics() = runBlocking {
        val phase = phaseWithTopics()
        env.progress.start(phase)
        env.progress.startTopic("t1")
        env.progress.completeTopic("t1")
        env.progress.startTopic("t2")
        env.progress.completeTopic("t2")
        env.progress.complete(phase)

        assertTrue(vm(phase).loaded().locked)
    }

    @Test
    fun deletingATopicRemovesItAndRenumbers() = runBlocking {
        val phase = phaseWithTopics()
        val vm = vm(phase)
        vm.loaded()

        vm.delete("t1")

        val rows = withTimeout(10_000) { vm.uiState.first { it.rows.size == 1 } }.rows
        assertEquals(listOf(1 to "Chapter 1"), rows.map { it.number to it.name })
    }

    @Test
    fun movingATopicReorders() = runBlocking {
        val phase = phaseWithTopics()
        val vm = vm(phase)
        vm.loaded()

        vm.move(from = 0, to = 1)

        val rows = withTimeout(10_000) { vm.uiState.first { it.rows.firstOrNull()?.name == "Chapter 1" } }.rows
        assertEquals(listOf(1 to "Chapter 1", 2 to "Greetings"), rows.map { it.number to it.name })
    }
}
