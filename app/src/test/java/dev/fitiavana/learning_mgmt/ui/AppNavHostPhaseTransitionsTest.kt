package dev.fitiavana.learning_mgmt.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.fitiavana.learning_mgmt.AppContainer
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase screens slide (unlike Home <-> Manage curricula, see [AppNavHostTransitionsTest]), so going
 * back from a phase to the list must not cut: the phase is still on screen a frame later, then gone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AppNavHostPhaseTransitionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val phaseOnlyText = "Complete Phase 1 first"

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Before
    fun setUp() {
        runBlocking {
            val container = AppContainer(ApplicationProvider.getApplicationContext<Context>())
            val spanish = container.curriculumRepository.create("Spanish")
            val basics = container.phaseRepository.add(spanish, "Basics", "")
            container.phaseRepository.add(spanish, "Verbs", "")
            container.progressRepository.start(basics)
            compose.setContent { LearningmgmtTheme { AppNavHost(container) } }
        }
        waitForText("Mark as completed")
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Phases").performClick()
        waitForText("Phase 2 · Verbs")
        compose.onNodeWithText("Phase 2 · Verbs").performClick()
        waitForText(phaseOnlyText)
        compose.mainClock.autoAdvance = false
    }

    @Test
    fun phaseViewSlidesOutInsteadOfCuttingWhenGoingBack() {
        compose.onNodeWithContentDescription("Back").performClick()
        compose.mainClock.advanceTimeBy(50)
        compose.onAllNodesWithText(phaseOnlyText).assertCountEquals(1)

        compose.mainClock.advanceTimeBy(1_000)
        compose.onAllNodesWithText(phaseOnlyText).assertCountEquals(0)
    }
}
