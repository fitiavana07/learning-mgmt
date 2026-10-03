package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class TopicsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val started = mutableListOf<String>()
    private val completed = mutableListOf<String>()
    private val recorded = mutableListOf<Pair<String, Int>>()

    private fun t(number: Int, status: Status, total: Int? = null, done: Int = 0, unit: String? = null) =
        TopicWithProgress(Topic("t$number", "p", number, "Name $number", total, unit), status, done)

    private fun show(topics: List<TopicWithProgress>, editable: Boolean = true) {
        compose.setContent {
            LearningmgmtTheme {
                TopicsSection(
                    topics = topics,
                    editable = editable,
                    handlers = TopicHandlers(
                        onStart = { started += it },
                        onComplete = { completed += it },
                        onRecord = { id, done -> recorded += id to done },
                    ),
                )
            }
        }
    }

    @Test
    fun showsNothingWithoutTopics() {
        show(emptyList())

        compose.onNodeWithText("Topics").assertDoesNotExist()
    }

    @Test
    fun showsTheOverallProgressAndEveryTopic() {
        show(listOf(t(1, Status.COMPLETED), t(2, Status.IN_PROGRESS), t(3, Status.NOT_STARTED)))

        compose.onNodeWithText("Topics").assertIsDisplayed()
        compose.onNodeWithText("1 of 3 topics completed").assertIsDisplayed()
        compose.onNodeWithTag("topics-progress").assertIsDisplayed()
        compose.onNodeWithText("Topic 1 · Name 1").assertIsDisplayed()
        compose.onNodeWithText("Topic 2 · Name 2").assertIsDisplayed()
        compose.onNodeWithText("Topic 3 · Name 3").assertIsDisplayed()
    }

    @Test
    fun theNextTopicCanBeStartedWhenNothingIsInProgress() {
        show(listOf(t(1, Status.COMPLETED), t(2, Status.NOT_STARTED), t(3, Status.NOT_STARTED)))

        compose.onNodeWithText("Start").performClick()

        assertEquals(listOf("t2"), started)
    }

    @Test
    fun theInProgressTopicCanBeCompletedAndNoOtherCanBeStarted() {
        show(listOf(t(1, Status.IN_PROGRESS), t(2, Status.NOT_STARTED)))

        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithText("Complete").performClick()

        assertEquals(listOf("t1"), completed)
    }

    @Test
    fun aQuantifiedTopicShowsItsProgressWithTheUnit() {
        show(listOf(t(1, Status.IN_PROGRESS, total = 40, done = 12, unit = "pages")))

        compose.onNodeWithText("12 / 40 pages").assertIsDisplayed()
        compose.onNodeWithTag("topic-progress-t1").assertIsDisplayed()
    }

    @Test
    fun aQuantifiedTopicWithoutAUnitShowsJustTheNumbers() {
        show(listOf(t(1, Status.IN_PROGRESS, total = 40, done = 12)))

        compose.onNodeWithText("12 / 40").assertIsDisplayed()
    }

    @Test
    fun theProgressOfTheCurrentQuantifiedTopicCanBeUpdated() {
        show(listOf(t(1, Status.IN_PROGRESS, total = 40, done = 12)))

        compose.onNodeWithTag("progress-field").performTextReplacement("25")
        compose.onNodeWithText("Update").performClick()

        assertEquals(listOf("t1" to 25), recorded)
    }

    @Test
    fun anInvalidProgressEntryIsNotReported() {
        show(listOf(t(1, Status.IN_PROGRESS, total = 40, done = 12)))

        compose.onNodeWithTag("progress-field").performTextReplacement("")
        compose.onNodeWithText("Update").performClick()

        assertEquals(emptyList<Pair<String, Int>>(), recorded)
    }

    @Test
    fun aSimpleCurrentTopicHasNoProgressField() {
        show(listOf(t(1, Status.IN_PROGRESS)))

        compose.onNodeWithTag("progress-field").assertDoesNotExist()
    }

    @Test
    fun readOnlyShowsTheTopicsWithoutAnyAction() {
        show(listOf(t(1, Status.IN_PROGRESS, total = 40, done = 12), t(2, Status.NOT_STARTED)), editable = false)

        compose.onNodeWithText("Topic 1 · Name 1").assertIsDisplayed()
        compose.onNodeWithText("12 / 40").assertIsDisplayed()
        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithText("Complete").assertDoesNotExist()
        compose.onNodeWithText("Update").assertDoesNotExist()
        compose.onNodeWithTag("progress-field").assertDoesNotExist()
    }
}
