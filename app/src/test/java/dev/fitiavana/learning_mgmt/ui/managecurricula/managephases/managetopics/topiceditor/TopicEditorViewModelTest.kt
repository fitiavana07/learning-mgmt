package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.topiceditor

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.topics.Topic
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
class TopicEditorViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var phase: String

    private fun editor(topicId: String? = null) = env.track(TopicEditorViewModel(phase, topicId, env.topics))

    private suspend fun TopicEditorViewModel.loaded() =
        withTimeout(10_000) { uiState.first { !it.loading } }

    private suspend fun saved(): List<Topic> = env.topics.observe(phase).first()

    private suspend fun TopicEditorViewModel.saveAndWait() {
        save()
        withTimeout(10_000) { uiState.first { it.saved } }
    }

    private suspend fun setUpPhase() {
        phase = env.phases.add(env.curricula.create("Spanish"), "Basics", "")
    }

    @Test
    fun aNewTopicStartsBlankSimpleAndUnchanged() = runBlocking {
        setUpPhase()
        val state = editor().loaded()

        assertTrue(state.isNew)
        assertEquals("", state.name)
        assertFalse(state.quantified)
        assertFalse(state.dirty)
    }

    @Test
    fun editingLoadsAQuantifiedTopic() = runBlocking {
        setUpPhase()
        val id = env.topics.add(phase, "Chapter 1", total = 40, unit = "pages")

        val state = editor(id).loaded()

        assertFalse(state.isNew)
        assertEquals("Chapter 1", state.name)
        assertTrue(state.quantified)
        assertEquals("40", state.total)
        assertEquals("pages", state.unit)
        assertFalse(state.dirty)
    }

    @Test
    fun anUnknownTopicIsNotFound() = runBlocking {
        setUpPhase()

        assertTrue(editor("nope").loaded().notFound)
    }

    @Test
    fun anyChangeMarksTheEditorDirty() = runBlocking {
        setUpPhase()
        val id = env.topics.add(phase, "Chapter 1")
        val vm = editor(id)
        vm.loaded()

        vm.onNameChange("Chapter 2")
        assertTrue(vm.uiState.value.dirty)
        vm.onNameChange("Chapter 1")
        assertFalse(vm.uiState.value.dirty)

        vm.onQuantifiedChange(true)
        assertTrue(vm.uiState.value.dirty)
    }

    @Test
    fun savingANewSimpleTopicAddsItAndMarksTheEditorSaved() = runBlocking {
        setUpPhase()
        val vm = editor()
        vm.loaded()

        vm.onNameChange("Greetings")
        vm.saveAndWait()

        assertTrue(vm.uiState.value.saved)
        assertEquals(listOf(Topic("t1", phase, 1, "Greetings", total = null, unit = null)), saved())
    }

    @Test
    fun savingANewQuantifiedTopicStoresTotalAndUnit() = runBlocking {
        setUpPhase()
        val vm = editor()
        vm.loaded()

        vm.onNameChange("Chapter 1")
        vm.onQuantifiedChange(true)
        vm.onTotalChange("40")
        vm.onUnitChange("pages")
        vm.saveAndWait()

        assertEquals(listOf(Topic("t1", phase, 1, "Chapter 1", total = 40, unit = "pages")), saved())
    }

    @Test
    fun aBlankNameIsRejected() = runBlocking {
        setUpPhase()
        val vm = editor()
        vm.loaded()

        vm.save()

        assertTrue(vm.uiState.value.nameError)
        assertFalse(vm.uiState.value.saved)
        assertEquals(emptyList<Topic>(), saved())
    }

    @Test
    fun aQuantifiedTopicNeedsAPositiveWholeTotal() = runBlocking {
        setUpPhase()
        val vm = editor()
        vm.loaded()
        vm.onNameChange("Chapter 1")
        vm.onQuantifiedChange(true)

        for (bad in listOf("", "0", "-3", "abc", "1.5")) {
            vm.onTotalChange(bad)
            vm.save()
            assertTrue("total '$bad' should be rejected", vm.uiState.value.totalError)
            assertFalse(vm.uiState.value.saved)
        }
        assertEquals(emptyList<Topic>(), saved())
    }

    @Test
    fun aSimpleTopicIgnoresAnyTypedTotal() = runBlocking {
        setUpPhase()
        val vm = editor()
        vm.loaded()
        vm.onNameChange("Greetings")
        vm.onQuantifiedChange(true)
        vm.onTotalChange("abc")
        vm.onQuantifiedChange(false)

        vm.saveAndWait()

        assertEquals(listOf(Topic("t1", phase, 1, "Greetings", total = null, unit = null)), saved())
    }

    @Test
    fun savingAnExistingTopicUpdatesIt() = runBlocking {
        setUpPhase()
        val id = env.topics.add(phase, "Chapter 1", total = 40, unit = "pages")
        val vm = editor(id)
        vm.loaded()

        vm.onTotalChange("60")
        vm.saveAndWait()

        assertEquals(listOf(Topic(id, phase, 1, "Chapter 1", total = 60, unit = "pages")), saved())
    }
}
