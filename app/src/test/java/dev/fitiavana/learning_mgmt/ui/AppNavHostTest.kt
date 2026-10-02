package dev.fitiavana.learning_mgmt.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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

/** The real container and navigation graph, driven through the UI. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AppNavHostTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var container: AppContainer

    @Before
    fun setUp() = runBlocking {
        container = AppContainer(ApplicationProvider.getApplicationContext<Context>())
        val spanish = container.curriculumRepository.create("Spanish")
        val basics = container.phaseRepository.add(spanish, "Basics", "")
        container.phaseRepository.add(spanish, "Verbs", "")
        container.progressRepository.start(basics)
        compose.setContent {
            LearningmgmtTheme { AppNavHost(container = container) }
        }
    }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun openPhasesList() {
        waitForText("Mark as completed")
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Phases").performClick()
        waitForText("Phase 2 · Verbs")
    }

    @Test
    fun launchesOnThePhaseInProgress() {
        waitForText("Mark as completed")

        compose.onNodeWithText("Mark as completed").assertIsDisplayed()
    }

    @Test
    fun overflowMenuOpensTheListOfPhases() {
        openPhasesList()

        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
        compose.onNodeWithText("Phase 2 · Verbs").assertIsDisplayed()
    }

    @Test
    fun openingALaterPhaseShowsItBlockedUntilTheCurrentOneIsCompleted() {
        openPhasesList()

        compose.onNodeWithText("Phase 2 · Verbs").performClick()
        waitForText("Complete Phase 1 first")

        compose.onNodeWithText("Start Phase 2").assertIsNotEnabled()
    }

    @Test
    fun completingFromThePhaseViewUnblocksTheNextPhase() {
        openPhasesList()
        compose.onNodeWithText("Phase 1 · Basics").performClick()
        waitForText("Mark as completed")

        compose.onNodeWithText("Mark as completed").performClick()
        compose.onNodeWithText("Complete").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Mark as completed").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        waitForText("Start Phase 2")
        compose.onNodeWithText("Ready to begin?").assertIsDisplayed()
    }

    private fun openManageCurricula() {
        waitForText("Mark as completed")
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithText("Manage curricula").performClick()
        waitForText("2 phases")
    }

    @Test
    fun drawerButtonOpensManageCurriculaListingCurriculaWithPhaseCounts() {
        openManageCurricula()

        compose.onNodeWithText("Spanish").assertIsDisplayed()
        compose.onNodeWithText("2 phases").assertIsDisplayed()
    }

    @Test
    fun deletingTheOnlyCurriculumFromManageCurriculaShowsTheEmptyList() {
        openManageCurricula()

        compose.onNodeWithContentDescription("Options for Spanish").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasClickAction()).performClick()

        waitForText("No curricula yet")
    }

    @Test
    fun deletingTheSelectedCurriculumFallsBackToTheFirstRemainingOne() {
        runBlocking { container.curriculumRepository.create("Piano") }
        openManageCurricula()

        compose.onNodeWithContentDescription("Options for Spanish").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasClickAction()).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Spanish").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription("Back").performClick()

        waitForText("No phases yet")
        compose.onAllNodesWithText("Spanish").assertCountEquals(0)
        waitForText("Piano")
    }

    @Test
    fun backFromManageCurriculaReturnsToHome() {
        openManageCurricula()

        compose.onNodeWithContentDescription("Back").performClick()

        waitForText("Mark as completed")
    }

    @Test
    fun backFromThePhasesListReturnsToHome() {
        openPhasesList()

        compose.onNodeWithContentDescription("Back").performClick()

        waitForText("Mark as completed")
    }
}
