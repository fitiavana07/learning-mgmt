package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ManageTopicsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val deleted = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private var adds = 0
    private val moves = mutableListOf<Pair<Int, Int>>()
    private var back = 0

    private val state = ManageTopicsState(
        phaseNumber = 2,
        phaseName = "Verbs",
        rows = listOf(
            TopicRow("t1", 1, "Greetings", Status.COMPLETED, total = null, done = 0, unit = null),
            TopicRow("t2", 2, "Chapter 1", Status.IN_PROGRESS, total = 40, done = 12, unit = "pages"),
            TopicRow("t3", 3, "Exercises", Status.NOT_STARTED, total = 10, done = 0, unit = null),
        ),
    )

    private fun show(state: ManageTopicsState = this.state) {
        compose.setContent {
            LearningmgmtTheme {
                ManageTopicsScreen(
                    state = state,
                    onAdd = { adds++ },
                    onTopicClick = { opened += it },
                    onDelete = { deleted += it },
                    onMove = { from, to -> moves += from to to },
                    onBack = { back++ },
                )
            }
        }
    }

    private fun openMenuOf(number: Int) =
        compose.onNodeWithContentDescription("Options for topic $number").performClick()

    @Test
    fun showsThePhaseAndNumberedTopics() {
        show()

        compose.onNodeWithText("Phase 2 · Verbs").assertIsDisplayed()
        compose.onNodeWithText("Topic 1 · Greetings").assertIsDisplayed()
        compose.onNodeWithText("Topic 2 · Chapter 1").assertIsDisplayed()
        compose.onNodeWithText("Topic 3 · Exercises").assertIsDisplayed()
    }

    @Test
    fun quantifiedTopicsShowTheirTotalWithTheUnitWhenThereIsOne() {
        show()

        compose.onNodeWithText("40 pages").assertIsDisplayed()
        compose.onNodeWithText("10").assertIsDisplayed()
    }

    @Test
    fun emptyListInvitesToAddATopic() {
        show(ManageTopicsState(phaseNumber = 2, phaseName = "Verbs"))

        compose.onNodeWithText("No topics yet").assertIsDisplayed()
        compose.onNodeWithText("Tap + to add one").assertIsDisplayed()
    }

    @Test
    fun addButtonAndBackAndRowTapsAreReported() {
        show()

        compose.onNodeWithContentDescription("New topic").performClick()
        compose.onNodeWithText("Topic 2 · Chapter 1").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, adds)
        assertEquals(listOf("t2"), opened)
        assertEquals(1, back)
    }

    @Test
    fun longPressingAndDraggingARowReportsTheMoveOnRelease() {
        show()

        compose.onNodeWithText("Topic 1 · Greetings").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, height * 2f))
            up()
        }

        assertEquals(listOf(0 to 2), moves)
    }

    @Test
    fun deleteAsksForConfirmationAndNamesTheTopic() {
        show()

        openMenuOf(3)
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("Delete Topic 3?").assertIsDisplayed()
        compose.onNodeWithText("Later topics are renumbered.", substring = true).assertIsDisplayed()
    }

    @Test
    fun confirmingDeleteReportsTheTopicId() {
        show()

        openMenuOf(1)
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasClickAction()).performClick()

        assertEquals(listOf("t1"), deleted)
    }

    @Test
    fun cancellingDeleteKeepsTheTopic() {
        show()

        openMenuOf(1)
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<String>(), deleted)
    }

    @Test
    fun aLockedPhaseCannotBeEdited() {
        show(state.copy(locked = true))

        compose.onNodeWithText("This phase is completed, so its topics can't be changed.").assertIsDisplayed()
        compose.onNodeWithContentDescription("New topic").assertDoesNotExist()
        compose.onNodeWithContentDescription("Options for topic 1").assertDoesNotExist()
    }
}
