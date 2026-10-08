package dev.fitiavana.learning_mgmt.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import dev.fitiavana.learning_mgmt.AppContainer
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import kotlinx.coroutines.flow.first
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
    fun theSyncScreenIsReachedFromTheManageCurriculaMenuAndBackAgain() {
        openManageCurricula()

        compose.onNodeWithContentDescription("Backup, restore and sync").performClick()
        compose.onNodeWithText("Sync with other devices").performClick()
        waitForText("Save passphrase")
        compose.onNodeWithText("Save passphrase").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
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

    private fun openManagePhases() {
        openManageCurricula()
        compose.onNodeWithText("Spanish").performClick()
        waitForText("Manage phases")
    }

    @Test
    fun tappingACurriculumOpensItsPhasesInOrder() {
        openManagePhases()

        compose.onNodeWithText("Spanish").assertIsDisplayed()
        waitForText("Phase 2 · Verbs")
        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
    }

    @Test
    fun deletingAPhaseFromManagePhasesRemovesItAndRenumbers() {
        openManagePhases()
        waitForText("Phase 2 · Verbs")

        compose.onNodeWithContentDescription("Options for phase 1").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasClickAction()).performClick()

        waitForText("Phase 1 · Verbs")
        compose.onAllNodesWithText("Phase 2 · Verbs").assertCountEquals(0)
    }

    @Test
    fun tappingAPhaseOpensItInManageModeWithoutStatusActions() {
        openManagePhases()
        waitForText("Phase 1 · Basics")

        compose.onNodeWithText("Phase 1 · Basics").performClick()

        waitForText("In progress")
        compose.onNodeWithContentDescription("Edit phase").assertIsDisplayed()
        compose.onNodeWithText("Mark as completed").assertDoesNotExist()
    }

    @Test
    fun editingAPhaseSavesItAndReturnsToItsView() {
        openManagePhases()
        waitForText("Phase 1 · Basics")
        compose.onNodeWithText("Phase 1 · Basics").performClick()
        waitForText("In progress")

        compose.onNodeWithContentDescription("Edit phase").performClick()
        waitForText("Edit phase")
        compose.onNodeWithTag("name-field").performTextReplacement("Foundations")
        compose.onNodeWithText("Save").performClick()

        waitForText("Phase 1 · Foundations")
        compose.onNodeWithContentDescription("Edit phase").assertIsDisplayed()
    }

    @Test
    fun addingAPhaseCreatesItAtTheEndOfTheList() {
        openManagePhases()
        waitForText("Phase 2 · Verbs")

        compose.onNodeWithContentDescription("New phase").performClick()
        waitForText("New phase")
        compose.onNodeWithTag("name-field").performTextReplacement("Tenses")
        compose.onNodeWithText("Save").performClick()

        waitForText("Phase 3 · Tenses")
        compose.onNodeWithText("Phase 2 · Verbs").assertIsDisplayed()
    }

    @Test
    fun savingANewPhaseWithoutANameShowsAnErrorAndStaysInTheEditor() {
        openManagePhases()
        waitForText("Phase 2 · Verbs")

        compose.onNodeWithContentDescription("New phase").performClick()
        waitForText("New phase")
        compose.onNodeWithText("Save").performClick()

        waitForText("Name can't be blank")
        compose.onNodeWithTag("name-field").assertIsDisplayed()
    }

    @Test
    fun leavingTheEditorWithChangesAsksBeforeDiscarding() {
        openManagePhases()
        waitForText("Phase 2 · Verbs")
        compose.onNodeWithContentDescription("New phase").performClick()
        waitForText("New phase")
        compose.onNodeWithTag("name-field").performTextReplacement("Draft")

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard").performClick()

        waitForText("Manage phases")
        compose.onAllNodesWithText("Phase 3 · Draft").assertCountEquals(0)
    }

    @Test
    fun homeBlocksCompletingThePhaseUntilItsTopicsAreDone() {
        waitForText("Mark as completed")
        runBlocking {
            val basics = container.progressRepository.observeAll().first().values.first().first().phase.id
            container.topicRepository.add(basics, "Greetings")
        }

        waitForText("Complete all topics first (0 of 1 done)")
        compose.onNodeWithText("Mark as completed").assertIsNotEnabled()
        compose.onNodeWithText("Start").performClick()
        waitForText("Complete")
        compose.onNodeWithText("Complete").performClick()

        waitForText("1 of 1 topic completed")
        compose.onNodeWithText("Mark as completed").assertIsEnabled()
    }

    private fun openTopicsOfBasics() {
        openManagePhases()
        waitForText("Phase 1 · Basics")
        compose.onNodeWithText("Phase 1 · Basics").performClick()
        waitForText("In progress")
        compose.onNodeWithContentDescription("Edit phase").performClick()
        waitForText("Edit phase")
        compose.onNodeWithText("Topics").performClick()
        waitForText("No topics yet")
    }

    @Test
    fun addingATopicFromThePhaseEditorListsItNumbered() {
        openTopicsOfBasics()

        compose.onNodeWithContentDescription("New topic").performClick()
        waitForText("Save")
        compose.onNodeWithTag("name-field").performTextReplacement("Greetings")
        compose.onNodeWithText("Save").performClick()

        waitForText("Topic 1 · Greetings")
        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
    }

    @Test
    fun addingAQuantifiedTopicKeepsItsTotalAndUnit() {
        openTopicsOfBasics()

        compose.onNodeWithContentDescription("New topic").performClick()
        waitForText("Quantified")
        compose.onNodeWithTag("name-field").performTextReplacement("Chapter 1")
        compose.onNodeWithText("Quantified").performClick()
        compose.onNodeWithTag("total-field").performTextReplacement("40")
        compose.onNodeWithTag("unit-field").performTextReplacement("pages")
        compose.onNodeWithText("Save").performClick()

        waitForText("Topic 1 · Chapter 1")
        compose.onNodeWithText("40 pages").assertIsDisplayed()
    }

    @Test
    fun backFromTheTopicsListReturnsToThePhaseEditor() {
        openTopicsOfBasics()

        compose.onNodeWithContentDescription("Back").performClick()

        waitForText("Edit phase")
    }

    @Test
    fun backFromManagePhasesReturnsToManageCurricula() {
        openManagePhases()

        compose.onNodeWithContentDescription("Back").performClick()

        waitForText("2 phases")
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
