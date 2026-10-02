package dev.fitiavana.learning_mgmt.ui.home.phases

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhasesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val clicked = mutableListOf<String>()
    private var back = 0

    private fun phase(number: Int, name: String, status: Status) =
        PhaseWithStatus(Phase("p$number", "c", number, name, ""), status)

    private fun show(state: PhasesUiState) {
        compose.setContent {
            LearningmgmtTheme {
                PhasesScreen(state = state, onPhaseClick = { clicked += it }, onBack = { back++ })
            }
        }
    }

    private val state = PhasesUiState(
        loading = false,
        curriculumName = "Spanish A2",
        phases = listOf(
            phase(1, "Basics", Status.COMPLETED),
            phase(2, "Past Tenses", Status.IN_PROGRESS),
            phase(3, "Subjunctive", Status.NOT_STARTED),
        ),
    )

    @Test
    fun listsEveryPhaseWithItsNumberAndName() {
        show(state)

        compose.onNodeWithText("Phases").assertIsDisplayed()
        compose.onNodeWithText("Spanish A2").assertIsDisplayed()
        compose.onNodeWithText("Phase 1 · Basics").assertIsDisplayed()
        compose.onNodeWithText("Phase 2 · Past Tenses").assertIsDisplayed()
        compose.onNodeWithText("Phase 3 · Subjunctive").assertIsDisplayed()
    }

    @Test
    fun showsAStatusIconPerPhase() {
        show(state)

        compose.onNodeWithContentDescription("Completed").assertIsDisplayed()
        compose.onNodeWithContentDescription("In progress").assertIsDisplayed()
        compose.onNodeWithContentDescription("Not started").assertIsDisplayed()
    }

    @Test
    fun tappingAPhaseReportsItsId() {
        show(state)

        compose.onNodeWithText("Phase 3 · Subjunctive").performClick()

        assertEquals(listOf("p3"), clicked)
    }

    @Test
    fun backButtonIsReported() {
        show(state)

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }

    @Test
    fun emptyCurriculumSaysNoPhasesYet() {
        show(PhasesUiState(loading = false, curriculumName = "Spanish A2", phases = emptyList()))

        compose.onNodeWithText("No phases yet").assertIsDisplayed()
    }

    @Test
    fun showsNothingWhileLoading() {
        show(PhasesUiState(loading = true, curriculumName = null, phases = emptyList()))

        compose.onNodeWithText("No phases yet").assertDoesNotExist()
    }
}
