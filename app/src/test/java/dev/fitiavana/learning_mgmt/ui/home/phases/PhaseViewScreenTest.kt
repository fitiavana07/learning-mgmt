package dev.fitiavana.learning_mgmt.ui.home.phases

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhaseViewScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var started = 0
    private var completed = 0
    private var back = 0

    private fun phase(number: Int, name: String, status: Status, description: String = "") =
        PhaseWithStatus(Phase("p$number", "c", number, name, description), status)

    private fun show(state: PhaseViewState) {
        compose.setContent {
            LearningmgmtTheme {
                PhaseViewScreen(
                    state = state,
                    onStart = { started++ },
                    onComplete = { completed++ },
                    onBack = { back++ },
                )
            }
        }
    }

    @Test
    fun showsTitleStatusAndRenderedDescription() {
        show(
            PhaseViewState.Loaded(
                phase(2, "Past Tenses", Status.COMPLETED, "## Goals"),
                PhaseAction.None,
            ),
        )

        compose.onNodeWithText("Phase 2 · Past Tenses").assertIsDisplayed()
        compose.onNodeWithText("Completed").assertIsDisplayed()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Goals").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mark as completed").assertDoesNotExist()
        compose.onNodeWithText("Start Phase 2").assertDoesNotExist()
    }

    @Test
    fun inProgressPhaseCanBeCompletedAfterConfirmation() {
        show(PhaseViewState.Loaded(phase(2, "Past Tenses", Status.IN_PROGRESS), PhaseAction.Complete))

        compose.onNodeWithText("Mark as completed").performClick()
        assertEquals(0, completed)
        compose.onNodeWithText("Complete").performClick()

        assertEquals(1, completed)
    }

    @Test
    fun nextPhaseCanBeStarted() {
        show(PhaseViewState.Loaded(phase(1, "Basics", Status.NOT_STARTED), PhaseAction.Start))

        compose.onNodeWithText("Start Phase 1").performClick()

        assertEquals(1, started)
    }

    @Test
    fun phaseBlockedByAnInProgressPhaseIsDisabledWithAHint() {
        val blocker = phase(1, "Basics", Status.IN_PROGRESS).phase
        show(PhaseViewState.Loaded(phase(2, "Verbs", Status.NOT_STARTED), PhaseAction.WaitForCompletion(blocker)))

        compose.onNodeWithText("Start Phase 2").assertIsNotEnabled()
        compose.onNodeWithText("Complete Phase 1 first").assertIsDisplayed()
    }

    @Test
    fun phaseBlockedByAnEarlierNotStartedPhaseIsDisabledWithAHint() {
        val blocker = phase(2, "Verbs", Status.NOT_STARTED).phase
        show(PhaseViewState.Loaded(phase(3, "Subjunctive", Status.NOT_STARTED), PhaseAction.WaitForStart(blocker)))

        compose.onNodeWithText("Start Phase 3").assertIsNotEnabled()
        compose.onNodeWithText("Start Phase 2 first").assertIsDisplayed()
    }

    @Test
    fun missingPhaseSaysSo() {
        show(PhaseViewState.NotFound)

        compose.onNodeWithText("Phase not found").assertIsDisplayed()
    }

    @Test
    fun backButtonIsReported() {
        show(PhaseViewState.NotFound)

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }
}
