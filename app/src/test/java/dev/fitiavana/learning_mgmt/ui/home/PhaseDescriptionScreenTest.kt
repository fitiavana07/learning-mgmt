package dev.fitiavana.learning_mgmt.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.home.phases.PhaseViewState
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class PhaseDescriptionScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var back = 0

    private fun show(state: PhaseViewState) {
        compose.setContent {
            LearningmgmtTheme { PhaseDescriptionScreen(state = state, onBack = { back++ }) }
        }
    }

    private fun loaded(description: String, goal: String = "") = PhaseViewState.Loaded(
        PhaseWithStatus(Phase("p2", "c", 2, "Past Tenses", description, goal), Status.IN_PROGRESS),
        PhaseAction.None,
    )

    @Test
    fun rendersTheDescriptionAsMarkdownUnderThePhaseTitle() {
        show(loaded("## Details\n- [ ] Finish exercises", goal = "Plain goal"))

        compose.onNodeWithText("Phase 2 · Past Tenses").assertIsDisplayed()
        // Markdown is parsed off the main thread, so wait for it to appear.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Details").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Details").assertIsDisplayed()
        compose.onNodeWithText("Finish exercises").assertIsDisplayed()
    }

    @Test
    fun backIsReported() {
        show(loaded("text"))

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }

    @Test
    fun anUnknownPhaseIsNotFound() {
        show(PhaseViewState.NotFound)

        compose.onNodeWithText("Phase not found").assertIsDisplayed()
    }
}
