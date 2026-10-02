package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class PhaseEditorScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val names = mutableListOf<String>()
    private val descriptions = mutableListOf<String>()
    private val previews = mutableListOf<Boolean>()
    private var saves = 0
    private var closes = 0

    private val editing = PhaseEditorState(
        loading = false,
        isNew = false,
        name = "Basics",
        description = "Some **bold** text",
        initialName = "Basics",
        initialDescription = "Some **bold** text",
    )

    private fun show(state: PhaseEditorState = editing) {
        compose.setContent {
            var current by remember { mutableStateOf(state) }
            LearningmgmtTheme {
                PhaseEditorScreen(
                    state = current,
                    onNameChange = { names += it; current = current.copy(name = it) },
                    onDescriptionChange = { descriptions += it; current = current.copy(description = it) },
                    onPreviewChange = { previews += it },
                    onSave = { saves++ },
                    onClose = { closes++ },
                )
            }
        }
    }

    @Test
    fun titleTellsNewFromExisting() {
        show(PhaseEditorState(loading = false))
        compose.onNodeWithText("New phase").assertIsDisplayed()
    }

    @Test
    fun editingShowsTheNameAndTheMarkdownSource() {
        show()

        compose.onNodeWithText("Edit phase").assertIsDisplayed()
        compose.onNodeWithText("Basics").assertIsDisplayed()
        compose.onNodeWithText("Some **bold** text").assertIsDisplayed()
    }

    @Test
    fun typingReportsNameAndDescriptionChanges() {
        show()

        compose.onNodeWithTag("name-field").performTextReplacement("Foundations")
        compose.onNodeWithTag("description-field").performTextReplacement("# Hello")

        assertEquals(listOf("Foundations"), names)
        assertEquals(listOf("# Hello"), descriptions)
    }

    @Test
    fun aBlankNameShowsAnError() {
        show(editing.copy(name = "", nameError = true))

        compose.onNodeWithText("Name can't be blank").assertIsDisplayed()
    }

    @Test
    fun saveIsReported() {
        show()

        compose.onNodeWithText("Save").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun theToggleReportsPreviewAndWrite() {
        show()
        compose.onNodeWithText("Preview").performClick()
        assertEquals(listOf(true), previews)
    }

    @Test
    fun previewRendersTheMarkdownInsteadOfTheEditor() {
        show(editing.copy(preview = true, description = "Some **bold** text"))

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Some bold text", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("description-field").assertDoesNotExist()
    }

    @Test
    fun leavingWithoutChangesClosesRightAway() {
        show()

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, closes)
        compose.onNodeWithText("Discard changes?").assertDoesNotExist()
    }

    @Test
    fun leavingWithChangesAsksBeforeDiscarding() {
        show(editing.copy(name = "Changed"))

        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Discard changes?").assertIsDisplayed()
        assertEquals(0, closes)
    }

    @Test
    fun discardingClosesTheEditor() {
        show(editing.copy(name = "Changed"))

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard").performClick()

        assertEquals(1, closes)
    }

    @Test
    fun cancellingTheDiscardKeepsEditing() {
        show(editing.copy(name = "Changed"))

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(0, closes)
        compose.onNodeWithText("Discard changes?").assertDoesNotExist()
    }

    @Test
    fun closesOnceSaved() {
        show(editing.copy(saved = true))

        compose.waitForIdle()

        assertEquals(1, closes)
    }

    @Test
    fun aMissingPhaseIsReported() {
        show(PhaseEditorState(loading = false, notFound = true, isNew = false))

        compose.onNodeWithText("Phase not found").assertIsDisplayed()
    }
}
