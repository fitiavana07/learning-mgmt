package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

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
class ManagePhasesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val deleted = mutableListOf<String>()
    private val moves = mutableListOf<Pair<Int, Int>>()
    private var back = 0

    private val state = ManagePhasesState(
        curriculumName = "Spanish",
        rows = listOf(
            PhaseRow("p1", 1, "Basics", Status.COMPLETED),
            PhaseRow("p2", 2, "Verbs", Status.IN_PROGRESS),
            PhaseRow("p3", 3, "Tenses", Status.NOT_STARTED),
        ),
    )

    private fun show(state: ManagePhasesState = this.state) {
        compose.setContent {
            LearningmgmtTheme {
                ManagePhasesScreen(
                    state = state,
                    onDelete = { deleted += it },
                    onMove = { from, to -> moves += from to to },
                    onBack = { back++ },
                )
            }
        }
    }

    @Test
    fun longPressingAndDraggingARowReportsTheMoveOnRelease() {
        show()

        compose.onNodeWithText("Phase 1 · Basics").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, height * 2f))
            assertEquals(emptyList<Pair<Int, Int>>(), moves)
            up()
        }

        assertEquals(listOf(0 to 2), moves)
    }

    @Test
    fun aQuickSwipeDoesNotStartADrag() {
        show()

        compose.onNodeWithText("Phase 1 · Basics").performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 2f))
            up()
        }

        assertEquals(emptyList<Pair<Int, Int>>(), moves)
    }

    private fun openMenuOf(number: Int) =
        compose.onNodeWithContentDescription("Options for phase $number").performClick()

    @Test
    fun showsTheCurriculumNameAndNumberedPhases() {
        show()

        compose.onNodeWithText("Spanish").assertIsDisplayed()
        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
        compose.onNodeWithText("Phase 2 · Verbs").assertIsDisplayed()
        compose.onNodeWithText("Phase 3 · Tenses").assertIsDisplayed()
    }

    @Test
    fun showsAStatusIconPerPhase() {
        show()

        compose.onNodeWithContentDescription("Completed").assertIsDisplayed()
        compose.onNodeWithContentDescription("In progress").assertIsDisplayed()
        compose.onNodeWithContentDescription("Not started").assertIsDisplayed()
    }

    @Test
    fun emptyListSaysThereAreNoPhases() {
        show(ManagePhasesState(curriculumName = "Spanish"))

        compose.onNodeWithText("No phases yet").assertIsDisplayed()
    }

    @Test
    fun backButtonIsReported() {
        show()

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }

    @Test
    fun deleteConfirmationNamesThePhase() {
        show()

        openMenuOf(3)
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("Delete Phase 3?").assertIsDisplayed()
        compose.onNodeWithText("Later phases are renumbered.").assertIsDisplayed()
        compose.onNodeWithText("This phase is in progress, and its progress will be lost.").assertDoesNotExist()
    }

    @Test
    fun deletingTheInProgressPhaseWarnsAboutLosingProgress() {
        show()

        openMenuOf(2)
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("This phase is in progress, and its progress will be lost.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun confirmingDeleteReportsThePhaseId() {
        show()

        openMenuOf(1)
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasClickAction()).performClick()

        assertEquals(listOf("p1"), deleted)
    }

    @Test
    fun cancellingDeleteKeepsThePhase() {
        show()

        openMenuOf(1)
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<String>(), deleted)
        compose.onNodeWithText("Delete Phase 1?").assertDoesNotExist()
    }
}
