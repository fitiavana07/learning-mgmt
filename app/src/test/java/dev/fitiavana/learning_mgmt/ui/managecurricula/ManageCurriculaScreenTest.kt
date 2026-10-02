package dev.fitiavana.learning_mgmt.ui.managecurricula

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ManageCurriculaScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val deleted = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private var back = 0

    private val rows = listOf(
        CurriculumRow("c1", "Spanish", phaseCount = 3),
        CurriculumRow("c2", "Piano", phaseCount = 1),
        CurriculumRow("c3", "Chess", phaseCount = 0),
    )

    private fun show(rows: List<CurriculumRow> = this.rows) {
        compose.setContent {
            LearningmgmtTheme {
                ManageCurriculaScreen(
                    rows = rows,
                    onCreate = {},
                    onRename = { _, _ -> },
                    onDelete = { deleted += it },
                    onCurriculumClick = { opened += it },
                    onBack = { back++ },
                )
            }
        }
    }

    private fun openMenuOf(name: String) =
        compose.onNodeWithContentDescription("Options for $name").performClick()

    @Test
    fun listsCurriculaWithTheirPhaseCount() {
        show()

        compose.onNodeWithText("Spanish").assertIsDisplayed()
        compose.onNodeWithText("3 phases").assertIsDisplayed()
        compose.onNodeWithText("1 phase").assertIsDisplayed()
        compose.onNodeWithText("0 phases").assertIsDisplayed()
    }

    @Test
    fun emptyListInvitesToCreateTheFirstCurriculum() {
        show(rows = emptyList())

        compose.onNodeWithText("No curricula yet").assertIsDisplayed()
    }

    @Test
    fun tappingARowReportsItsId() {
        show()

        compose.onNodeWithText("Piano").performClick()

        assertEquals(listOf("c2"), opened)
    }

    @Test
    fun backButtonIsReported() {
        show()

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }

    // Dialogs with a text field (create, rename) are not tested here: under Robolectric a text
    // field inside a dialog window never lets Compose go idle. They are on the manual checklist.

    @Test
    fun deleteConfirmationStatesThePhaseCount() {
        show()

        openMenuOf("Spanish")
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("Delete Spanish?").assertIsDisplayed()
        compose.onNodeWithText("This also deletes its 3 phases and their progress.").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmationSingularPhase() {
        show()

        openMenuOf("Piano")
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("This also deletes its 1 phase and its progress.").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmationForACurriculumWithoutPhases() {
        show()

        openMenuOf("Chess")
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("This curriculum has no phases.").assertIsDisplayed()
    }

    @Test
    fun confirmingDeleteReportsTheId() {
        show()

        openMenuOf("Spanish")
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasClickAction()).performClick()

        assertEquals(listOf("c1"), deleted)
    }

    @Test
    fun cancellingDeleteKeepsTheCurriculum() {
        show()

        openMenuOf("Spanish")
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<String>(), deleted)
        compose.onNodeWithText("Delete Spanish?").assertDoesNotExist()
    }
}
