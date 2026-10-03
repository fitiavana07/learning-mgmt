package dev.fitiavana.learning_mgmt.ui.home.phases

import dev.fitiavana.learning_mgmt.ui.common.TopicHandlers
import dev.fitiavana.learning_mgmt.features.topics.Topic
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress
import dev.fitiavana.learning_mgmt.features.progress.TopicSummary
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

    private var edited = 0

    private fun show(state: PhaseViewState, editable: Boolean = false) {
        compose.setContent {
            LearningmgmtTheme {
                PhaseViewScreen(
                    state = state,
                    onStart = { started++ },
                    onComplete = { completed++ },
                    onBack = { back++ },
                    onEdit = if (editable) ({ edited++ }) else null,
                )
            }
        }
    }

    @Test
    fun hasNoEditButtonInReadMode() {
        show(PhaseViewState.Loaded(phase(1, "Basics", Status.NOT_STARTED), PhaseAction.Start))

        compose.onNodeWithContentDescription("Edit phase").assertDoesNotExist()
    }

    @Test
    fun editButtonIsReportedInManageMode() {
        show(PhaseViewState.Loaded(phase(1, "Basics", Status.NOT_STARTED), PhaseAction.None), editable = true)

        compose.onNodeWithContentDescription("Edit phase").performClick()

        assertEquals(1, edited)
    }

    @Test
    fun editButtonIsHiddenWhenThePhaseIsMissing() {
        show(PhaseViewState.NotFound, editable = true)

        compose.onNodeWithContentDescription("Edit phase").assertDoesNotExist()
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

    private fun topic(number: Int, status: Status) =
        TopicWithProgress(Topic("t$number", "p1", number, "Name $number", null, null), status, 0)

    private val startedTopics = mutableListOf<String>()

    private fun showWithTopics(state: PhaseViewState, editable: Boolean) {
        compose.setContent {
            LearningmgmtTheme {
                PhaseViewScreen(
                    state = state,
                    onStart = {},
                    onComplete = {},
                    onBack = {},
                    onEdit = if (editable) ({}) else null,
                    topicHandlers = TopicHandlers(onStart = { startedTopics += it }, onComplete = {}, onRecord = { _, _ -> }),
                )
            }
        }
    }

    @Test
    fun anInProgressPhaseOffersTheTopicActions() {
        showWithTopics(
            PhaseViewState.Loaded(
                phase(1, "Basics", Status.IN_PROGRESS),
                PhaseAction.WaitForTopics(TopicSummary(0, 1)),
                listOf(topic(1, Status.NOT_STARTED)),
            ),
            editable = false,
        )

        compose.onNodeWithText("Topic 1 · Name 1").assertIsDisplayed()
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Mark as completed").assertIsNotEnabled()

        assertEquals(listOf("t1"), startedTopics)
    }

    @Test
    fun aPhaseThatIsNotInProgressShowsItsTopicsReadOnly() {
        showWithTopics(
            PhaseViewState.Loaded(phase(1, "Basics", Status.NOT_STARTED), PhaseAction.Start, listOf(topic(1, Status.NOT_STARTED))),
            editable = false,
        )

        compose.onNodeWithText("Topic 1 · Name 1").assertIsDisplayed()
        compose.onNodeWithText("Start").assertDoesNotExist()
    }

    @Test
    fun manageModeShowsTheTopicsReadOnlyEvenWhileInProgress() {
        showWithTopics(
            PhaseViewState.Loaded(phase(1, "Basics", Status.IN_PROGRESS), PhaseAction.None, listOf(topic(1, Status.NOT_STARTED))),
            editable = true,
        )

        compose.onNodeWithText("Topic 1 · Name 1").assertIsDisplayed()
        compose.onNodeWithText("Start").assertDoesNotExist()
    }
}
