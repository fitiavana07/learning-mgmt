package dev.fitiavana.learning_mgmt.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import dev.fitiavana.learning_mgmt.features.selection.testSelectionStore
import dev.fitiavana.learning_mgmt.ui.home.drawer.DrawerViewModel
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Home plus drawer wired to real ViewModels over an in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class HomeRouteTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var managed = 0

    @Before
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        val curricula = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
        val phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        val progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao(), db.topicProgressDao())
        val spanish = curricula.create("Spanish")
        phases.add(spanish, "Basics", "")
        progress.start("p1")
        val piano = curricula.create("Piano")
        phases.add(piano, "Scales", "")

        val selection = CurriculumSelection(curricula, testSelectionStore(folder.root, scope))
        val homeViewModel = HomeViewModel(selection, progress)
        val drawerViewModel = DrawerViewModel(curricula, selection, progress)
        compose.setContent {
            LearningmgmtTheme {
                HomeRoute(
                    home = homeViewModel,
                    drawer = drawerViewModel,
                    onManageCurricula = { managed++ },
                    onShowPhases = {},
                )
            }
        }
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    @Test
    fun opensOnTheFirstCurriculumsPhaseInProgress() {
        compose.waitUntil(5_000) { count("Mark as completed") > 0 }

        compose.onNodeWithText("Mark as completed").assertIsDisplayed()
    }

    @Test
    fun selectingAnotherCurriculumInTheDrawerSwitchesHome() {
        compose.waitUntil(5_000) { count("Phase 1 · Basics") > 0 }

        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.waitUntil(5_000) { count("Piano") > 0 }
        compose.onNodeWithText("Piano").performClick()

        compose.waitUntil(5_000) { count("Start Phase 1") > 0 }
        compose.onNodeWithText("Phase 1 · Scales").assertIsDisplayed()
    }

    @Test
    fun manageCurriculaInTheDrawerIsReported() {
        compose.waitUntil(5_000) { count("Phase 1 · Basics") > 0 }

        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.waitUntil(5_000) { count("Manage curricula") > 0 }
        compose.onNodeWithText("Manage curricula").performClick()

        assertEquals(1, managed)
    }
}
