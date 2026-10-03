package dev.fitiavana.learning_mgmt.ui.home

import dev.fitiavana.learning_mgmt.ui.showKeyboard
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.LocalView
import android.view.View
import dev.fitiavana.learning_mgmt.ui.common.TopicHandlers
import dev.fitiavana.learning_mgmt.features.topics.Topic
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress
import dev.fitiavana.learning_mgmt.features.progress.Status
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var started = 0
    private var completed = 0
    private var managed = 0
    private var menuOpened = 0
    private var phasesShown = 0

    private fun show(state: HomeUiState) {
        compose.setContent {
            LearningmgmtTheme {
                HomeScreen(
                    state = state,
                    onStart = { started++ },
                    onComplete = { completed++ },
                    onManageCurricula = { managed++ },
                    onOpenMenu = { menuOpened++ },
                    onShowPhases = { phasesShown++ },
                )
            }
        }
    }

    private fun phase(number: Int, name: String, description: String = "") =
        Phase("p$number", "c", number, name, description)

    @Test
    fun inProgressShowsTitleStatusDescriptionAndCompleteButton() {
        show(
            HomeUiState(
                "Spanish A2",
                HomeContent.InProgress(phase(2, "Past Tenses", "## Goals\n- [ ] Finish exercises")),
            ),
        )

        compose.onNodeWithText("Spanish A2").assertIsDisplayed()
        compose.onNodeWithText("Phase 2 · Past Tenses").assertIsDisplayed()
        compose.onNodeWithText("In progress").assertIsDisplayed()
        // Markdown is parsed off the main thread, so wait for it to appear.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Goals").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Goals").assertIsDisplayed()
        compose.onNodeWithText("Finish exercises").assertIsDisplayed()
        compose.onNodeWithText("Mark as completed").assertIsDisplayed()
    }

    @Test
    fun completingAsksForConfirmationFirst() {
        show(HomeUiState("Spanish", HomeContent.InProgress(phase(2, "Past Tenses"))))

        compose.onNodeWithText("Mark as completed").performClick()

        compose.onNodeWithText("Mark Phase 2 as completed?").assertIsDisplayed()
        assertEquals(0, completed)

        compose.onNodeWithText("Complete").performClick()

        assertEquals(1, completed)
        compose.onNodeWithText("Mark Phase 2 as completed?").assertDoesNotExist()
    }

    @Test
    fun cancellingTheConfirmationCompletesNothing() {
        show(HomeUiState("Spanish", HomeContent.InProgress(phase(2, "Past Tenses"))))

        compose.onNodeWithText("Mark as completed").performClick()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(0, completed)
        compose.onNodeWithText("Mark Phase 2 as completed?").assertDoesNotExist()
    }

    @Test
    fun readyToStartOffersTheStartButton() {
        show(HomeUiState("Spanish", HomeContent.ReadyToStart(phase(1, "Basics"))))

        compose.onNodeWithText("Ready to begin?").assertIsDisplayed()
        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
        compose.onNodeWithText("Start Phase 1").performClick()

        assertEquals(1, started)
    }

    @Test
    fun noCurriculaPointsToManageCurricula() {
        show(HomeUiState(null, HomeContent.NoCurricula))

        compose.onNodeWithText("Create your first curriculum").assertIsDisplayed()
        compose.onNodeWithText("Manage curricula").performClick()

        assertEquals(1, managed)
    }

    @Test
    fun noPhasesExplainsWhereToAddThem() {
        show(HomeUiState("Spanish", HomeContent.NoPhases))

        compose.onNodeWithText("No phases yet").assertIsDisplayed()
        compose.onNodeWithText("Manage curricula").performClick()

        assertEquals(1, managed)
    }

    @Test
    fun allCompletedSaysSo() {
        show(HomeUiState("Spanish", HomeContent.AllCompleted))

        compose.onNodeWithText("All phases completed").assertIsDisplayed()
    }

    @Test
    fun menuButtonOpensTheDrawer() {
        show(HomeUiState("Spanish", HomeContent.AllCompleted))

        compose.onNodeWithContentDescription("Open menu").performClick()

        assertEquals(1, menuOpened)
    }

    @Test
    fun overflowMenuLeadsToThePhasesList() {
        show(HomeUiState("Spanish", HomeContent.AllCompleted))

        compose.onNodeWithText("Phases").assertDoesNotExist()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Phases").performClick()

        assertEquals(1, phasesShown)
    }

    @Test
    fun loadingShowsNoActions() {
        show(HomeUiState(null, HomeContent.Loading))

        compose.onNodeWithText("Mark as completed").assertDoesNotExist()
        compose.onNodeWithText("Manage curricula").assertDoesNotExist()
    }

    private val LONG_DESCRIPTION = (1..120).joinToString("\n\n") { "Paragraph number $it" }

    private val startedTopics = mutableListOf<String>()

    private fun topic(number: Int, status: Status) =
        TopicWithProgress(Topic("t$number", "p2", number, "Name $number", null, null), status, 0)

    private fun showWithTopics(state: HomeUiState) {
        compose.setContent {
            LearningmgmtTheme {
                HomeScreen(
                    state = state,
                    onStart = { started++ },
                    onComplete = { completed++ },
                    onManageCurricula = { managed++ },
                    onOpenMenu = { menuOpened++ },
                    onShowPhases = { phasesShown++ },
                    topicHandlers = TopicHandlers(onStart = { startedTopics += it }, onComplete = {}, onRecord = { _, _ -> }),
                )
            }
        }
    }

    @Test
    fun inProgressShowsTheTopicsAndStartsTheNextOne() {
        showWithTopics(
            HomeUiState(
                "Spanish",
                HomeContent.InProgress(phase(2, "Past Tenses"), listOf(topic(1, Status.NOT_STARTED), topic(2, Status.NOT_STARTED))),
            ),
        )

        compose.onNodeWithText("0 of 2 topics completed").assertExists()
        compose.onNodeWithText("Topic 1 · Name 1").assertExists()
        compose.onNodeWithText("Start").performClick()

        assertEquals(listOf("t1"), startedTopics)
    }

    @Test
    fun theProgressFieldStaysAboveTheKeyboard() {
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            LearningmgmtTheme {
                HomeScreen(
                    state = HomeUiState(
                        "Spanish",
                        HomeContent.InProgress(
                            phase(2, "Past Tenses", LONG_DESCRIPTION),
                            listOf(
                                TopicWithProgress(
                                    Topic("t1", "p2", 1, "Chapter", total = 40, unit = "pages"),
                                    Status.IN_PROGRESS,
                                    done = 12,
                                ),
                            ),
                        ),
                    ),
                    onStart = {},
                    onComplete = {},
                    onManageCurricula = {},
                    onOpenMenu = {},
                    onShowPhases = {},
                )
            }
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Show more").fetchSemanticsNodes().isNotEmpty() }
        // Like on a device: scroll to the field and tap it, then the keyboard slides in.
        compose.onNodeWithTag("progress-field").performScrollTo().performClick()
        val visibleBottom = compose.showKeyboard(view, heightPx = 400)

        // positionInRoot is not clipped to the scroll viewport, unlike boundsInRoot.
        val field = compose.onNodeWithTag("progress-field").fetchSemanticsNode()
        val fieldBottom = field.positionInRoot.y + field.size.height
        assertTrue("Field ($fieldBottom) hidden below $visibleBottom", fieldBottom <= visibleBottom)
    }

    @Test
    fun theCompleteButtonWaitsForUncompletedTopics() {
        showWithTopics(
            HomeUiState("Spanish", HomeContent.InProgress(phase(2, "Past Tenses"), listOf(topic(1, Status.IN_PROGRESS)))),
        )

        compose.onNodeWithText("Mark as completed").assertIsNotEnabled()
        compose.onNodeWithText("Complete all topics first (0 of 1 done)").assertIsDisplayed()
    }

    @Test
    fun theCompleteButtonIsEnabledOnceAllTopicsAreCompleted() {
        showWithTopics(
            HomeUiState("Spanish", HomeContent.InProgress(phase(2, "Past Tenses"), listOf(topic(1, Status.COMPLETED)))),
        )

        compose.onNodeWithText("Mark as completed").assertIsEnabled()
    }

    @Test
    fun aPhaseReadyToStartShowsItsTopicsWithoutActions() {
        showWithTopics(
            HomeUiState("Spanish", HomeContent.ReadyToStart(phase(2, "Past Tenses"), listOf(topic(1, Status.NOT_STARTED)))),
        )

        compose.onNodeWithText("Topic 1 · Name 1").assertExists()
        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithText("Start Phase 2").assertIsDisplayed()
    }
}
