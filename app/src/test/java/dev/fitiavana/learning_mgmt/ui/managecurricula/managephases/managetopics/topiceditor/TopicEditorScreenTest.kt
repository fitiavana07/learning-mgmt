package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.topiceditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class TopicEditorScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val names = mutableListOf<String>()
    private val totals = mutableListOf<String>()
    private val units = mutableListOf<String>()
    private val kinds = mutableListOf<Boolean>()
    private var saves = 0
    private var closes = 0

    private fun show(state: TopicEditorState = TopicEditorState(loading = false)) {
        compose.setContent {
            var current by remember { mutableStateOf(state) }
            LearningmgmtTheme {
                TopicEditorScreen(
                    state = current,
                    onNameChange = { names += it; current = current.copy(name = it) },
                    onQuantifiedChange = { kinds += it; current = current.copy(quantified = it) },
                    onTotalChange = { totals += it; current = current.copy(total = it) },
                    onUnitChange = { units += it; current = current.copy(unit = it) },
                    onSave = { saves++ },
                    onClose = { closes++ },
                )
            }
        }
    }

    @Test
    fun titleTellsNewFromExisting() {
        show()
        compose.onNodeWithText("New topic").assertIsDisplayed()
    }

    @Test
    fun editingShowsTheExistingNameAndAnEditTitle() {
        show(TopicEditorState(loading = false, isNew = false, name = "Chapter 1", initialName = "Chapter 1"))

        compose.onNodeWithText("Edit topic").assertIsDisplayed()
        compose.onNodeWithText("Chapter 1").assertIsDisplayed()
    }

    @Test
    fun aSimpleTopicHidesTheTotalAndUnitFields() {
        show()

        compose.onNodeWithTag("total-field").assertDoesNotExist()
        compose.onNodeWithTag("unit-field").assertDoesNotExist()
    }

    @Test
    fun choosingQuantifiedRevealsTheTotalAndUnitFields() {
        show()

        compose.onNodeWithText("Quantified").performClick()

        assertEquals(listOf(true), kinds)
        compose.onNodeWithTag("total-field").assertIsDisplayed()
        compose.onNodeWithTag("unit-field").assertIsDisplayed()
    }

    @Test
    fun typingReportsNameTotalAndUnit() {
        show(TopicEditorState(loading = false, quantified = true))

        compose.onNodeWithTag("name-field").performTextReplacement("Chapter 1")
        compose.onNodeWithTag("total-field").performTextReplacement("40")
        compose.onNodeWithTag("unit-field").performTextReplacement("pages")

        assertEquals(listOf("Chapter 1"), names)
        assertEquals(listOf("40"), totals)
        assertEquals(listOf("pages"), units)
    }

    @Test
    fun saveAndBackAreReported() {
        show()

        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, saves)
        assertEquals(1, closes)
    }

    @Test
    fun errorsAreShownUnderTheFields() {
        show(TopicEditorState(loading = false, quantified = true, nameError = true, totalError = true))

        compose.onNodeWithText("Name can't be blank").assertIsDisplayed()
        compose.onNodeWithText("Enter a whole number above 0").assertIsDisplayed()
    }

    @Test
    fun anUnknownTopicSaysSo() {
        show(TopicEditorState(loading = false, notFound = true, isNew = false))

        compose.onNodeWithText("Topic not found").assertIsDisplayed()
    }

    @Test
    fun closingWithUnsavedChangesAsksBeforeDiscarding() {
        show(TopicEditorState(loading = false, name = "x", initialName = ""))

        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Discard changes?").assertIsDisplayed()
        assertEquals(0, closes)
    }
}
