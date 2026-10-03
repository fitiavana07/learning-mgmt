package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Screen is 800dp tall, so the collapsed description is at most 320dp (40%). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class PhaseContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(description: String) {
        compose.setContent {
            LearningmgmtTheme {
                PhaseContent(
                    phase = Phase("p1", "c1", 1, "Basics", description),
                    header = { Text("header") },
                    footer = { Text("footer") },
                )
            }
        }
    }

    private val long = (1..120).joinToString("\n\n") { "Paragraph number $it" }

    private fun descriptionHeightDp(): Float =
        with(compose.density) { compose.onNodeWithTag("phase-description").fetchSemanticsNode().size.height.toDp().value }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun aShortDescriptionIsShownInFullWithoutAToggle() {
        show("Just **one** line")

        waitForText("Just one line")
        compose.onNodeWithText("Show more").assertDoesNotExist()
        compose.onNodeWithText("Show less").assertDoesNotExist()
    }

    @Test
    fun aLongDescriptionIsCollapsedToFortyPercentOfTheScreen() {
        show(long)

        waitForText("Show more")

        assertTrue(descriptionHeightDp() <= 320.5f)
        compose.onNodeWithTag("phase-description").assertHeightIsAtLeast(300.dp)
    }

    @Test
    fun showMoreExpandsAndShowLessCollapsesAgain() {
        show(long)
        waitForText("Show more")

        compose.onNodeWithText("Show more").performClick()
        compose.onNodeWithText("Show less").assertExists()
        compose.onNodeWithText("Show more").assertDoesNotExist()
        compose.onNodeWithTag("phase-description").assertHeightIsAtLeast(321.dp)

        compose.onNodeWithText("Show less").performScrollTo().performClick()
        compose.onNodeWithText("Show more").assertIsDisplayed()
        assertTrue(descriptionHeightDp() <= 320.5f)
    }

    @Test
    fun theHeaderTitleAndFooterAreAlwaysShown() {
        show(long)

        compose.onNodeWithText("header").assertIsDisplayed()
        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
        waitForText("Show more")
        compose.onNodeWithText("footer").assertExists()
    }
}
