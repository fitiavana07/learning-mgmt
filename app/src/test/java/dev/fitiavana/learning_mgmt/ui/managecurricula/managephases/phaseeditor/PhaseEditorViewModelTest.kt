package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import dev.fitiavana.learning_mgmt.db.TestEnvironment
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
class PhaseEditorViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private fun editor(curriculumId: String, phaseId: String? = null) =
        env.track(PhaseEditorViewModel(curriculumId, phaseId, env.phases))

    private suspend fun PhaseEditorViewModel.loaded() =
        withTimeout(10_000) { uiState.first { !it.loading } }

    private suspend fun spanishWithBasics(): String {
        val spanish = env.curricula.create("Spanish")
        env.phases.add(spanish, "Basics", "# Intro")
        return spanish
    }

    @Test
    fun aNewPhaseStartsBlankAndUnchanged() = runBlocking {
        val state = editor(spanishWithBasics()).loaded()

        assertTrue(state.isNew)
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertFalse(state.dirty)
    }

    @Test
    fun editingLoadsTheExistingPhase() = runBlocking {
        val state = editor(spanishWithBasics(), "p1").loaded()

        assertFalse(state.isNew)
        assertEquals("Basics", state.name)
        assertEquals("# Intro", state.description)
        assertFalse(state.dirty)
    }

    @Test
    fun anUnknownPhaseIsNotFound() = runBlocking {
        assertTrue(editor(spanishWithBasics(), "nope").loaded().notFound)
    }

    @Test
    fun changingTheNameOrDescriptionMarksTheEditorDirty() = runBlocking {
        val vm = editor(spanishWithBasics(), "p1")
        vm.loaded()

        vm.onNameChange("Basics 2")
        assertTrue(vm.uiState.value.dirty)

        vm.onNameChange("Basics")
        assertFalse(vm.uiState.value.dirty)

        vm.onDescriptionChange("# Changed")
        assertTrue(vm.uiState.value.dirty)
    }

    @Test
    fun togglingPreviewDoesNotMakeTheEditorDirty() = runBlocking {
        val vm = editor(spanishWithBasics(), "p1")
        vm.loaded()

        vm.setPreview(true)

        assertTrue(vm.uiState.value.preview)
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun savingANewPhaseAppendsItAtTheEnd() = runBlocking {
        val spanish = spanishWithBasics()
        val vm = editor(spanish)
        vm.loaded()

        vm.onNameChange("  Verbs ")
        vm.onDescriptionChange("Regular verbs")
        vm.save()

        withTimeout(10_000) { vm.uiState.first { it.saved } }
        val phases = env.phases.observe(spanish).first()
        assertEquals(listOf(1 to "Basics", 2 to "Verbs"), phases.map { it.number to it.name })
        assertEquals("Regular verbs", phases.last().description)
    }

    @Test
    fun savingAnExistingPhaseUpdatesIt() = runBlocking {
        val spanish = spanishWithBasics()
        val vm = editor(spanish, "p1")
        vm.loaded()

        vm.onNameChange("Foundations")
        vm.onDescriptionChange("New text")
        vm.save()

        withTimeout(10_000) { vm.uiState.first { it.saved } }
        val phase = env.phases.observe(spanish).first().single()
        assertEquals(1, phase.number)
        assertEquals("Foundations", phase.name)
        assertEquals("New text", phase.description)
    }

    @Test
    fun aBlankNameIsRejectedAndNothingIsSaved() = runBlocking {
        val spanish = spanishWithBasics()
        val vm = editor(spanish)
        vm.loaded()

        vm.onNameChange("   ")
        vm.save()

        assertTrue(vm.uiState.value.nameError)
        assertFalse(vm.uiState.value.saved)
        assertEquals(1, env.phases.observe(spanish).first().size)
    }

    @Test
    fun typingANameClearsTheError() = runBlocking {
        val vm = editor(spanishWithBasics())
        vm.loaded()
        vm.save()
        assertTrue(vm.uiState.value.nameError)

        vm.onNameChange("V")

        assertFalse(vm.uiState.value.nameError)
    }
}
