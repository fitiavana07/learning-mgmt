package dev.fitiavana.learning_mgmt.ui.home.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress.Summary
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1200dp") // tall enough to show all entries without scrolling
class AppDrawerContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val selectedIds = mutableListOf<String>()
    private var managed = 0

    private fun phase(number: Int, name: String) = Phase("p$number", "c", number, name, "")

    private fun item(id: String, name: String, progress: CurriculumProgress, selected: Boolean = false) =
        DrawerItem(id, name, progress, selected)

    private fun show(items: List<DrawerItem>) {
        compose.setContent {
            LearningmgmtTheme {
                AppDrawerContent(
                    items = items,
                    versionName = "1.2.3",
                    onSelect = { selectedIds += it },
                    onManageCurricula = { managed++ },
                )
            }
        }
    }

    private val items = listOf(
        item("a", "Spanish A2", CurriculumProgress(2, 8, Summary.InProgress(phase(2, "Past Tenses"))), selected = true),
        item("b", "Kotlin", CurriculumProgress(4, 6, Summary.Next(phase(5, "Coroutines")))),
        item("c", "Piano", CurriculumProgress(0, 5, Summary.NotStarted)),
        item("d", "Chess", CurriculumProgress(3, 3, Summary.AllCompleted)),
        item("e", "Empty", CurriculumProgress(0, 0, Summary.NoPhases)),
    )

    @Test
    fun showsNameSummaryAndCountForEachCurriculum() {
        show(items)

        compose.onNodeWithText("Spanish A2").assertIsDisplayed()
        compose.onNodeWithText("Phase 2 · Past Tenses").assertIsDisplayed()
        compose.onNodeWithText("2/8").assertIsDisplayed()
        compose.onNodeWithText("Next: Phase 5 · Coroutines").assertIsDisplayed()
        compose.onNodeWithText("4/6").assertIsDisplayed()
        compose.onNodeWithText("Not started").assertIsDisplayed()
        compose.onNodeWithText("0/5").assertIsDisplayed()
        compose.onNodeWithText("All phases completed").assertIsDisplayed()
        compose.onNodeWithText("3/3").assertIsDisplayed()
        compose.onNodeWithText("No phases yet").assertIsDisplayed()
        compose.onNodeWithText("0/0").assertIsDisplayed()
    }

    @Test
    fun showsTheAppVersionNextToTheAppName() {
        show(emptyList())

        compose.onNodeWithText("[dev] learning-mgmt · v1.2.3").assertIsDisplayed()
    }

    @Test
    fun marksOnlyTheSelectedCurriculum() {
        show(items)

        compose.onNodeWithText("Spanish A2").assertIsSelected()
        compose.onNodeWithText("Kotlin").assertIsNotSelected()
    }

    @Test
    fun tappingACurriculumSelectsIt() {
        show(items)

        compose.onNodeWithText("Kotlin").performClick()

        assertEquals(listOf("b"), selectedIds)
    }

    @Test
    fun manageCurriculaButtonComesAfterTheCurricula() {
        show(items)

        compose.onNodeWithText("Manage curricula").assertIsDisplayed().performClick()

        assertEquals(1, managed)
    }

    @Test
    fun manageCurriculaIsAvailableWithoutAnyCurriculum() {
        show(emptyList())

        compose.onNodeWithText("Manage curricula").assertIsDisplayed()
    }
}
