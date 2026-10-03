package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.TopicSummary
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhaseActionBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(action: PhaseAction) {
        compose.setContent {
            LearningmgmtTheme {
                PhaseActionBar(Phase("p1", "c", 2, "Verbs", ""), action, onStart = {}, onComplete = {})
            }
        }
    }

    @Test
    fun waitingForTopicsShowsADisabledCompleteButtonWithTheTopicCount() {
        show(PhaseAction.WaitForTopics(TopicSummary(completed = 1, total = 3)))

        compose.onNodeWithText("Mark as completed").assertIsNotEnabled()
        compose.onNodeWithText("Complete all topics first (1 of 3 done)").assertIsDisplayed()
    }

    @Test
    fun completeStaysEnabledWhenNothingIsBlocking() {
        show(PhaseAction.Complete)

        compose.onNodeWithText("Mark as completed").assertIsEnabled()
    }
}
