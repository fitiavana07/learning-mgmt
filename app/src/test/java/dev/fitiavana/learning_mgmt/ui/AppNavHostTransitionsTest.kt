package dev.fitiavana.learning_mgmt.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.fitiavana.learning_mgmt.AppContainer
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Screens switch without a crossfade: on a real device, opening the drawer while the Home <-> Manage
 * curricula crossfade was still running left the window blank (flat gray). So the outgoing screen
 * must be gone one short frame after navigating, never composed alongside the incoming one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AppNavHostTransitionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val homeOnlyText = "Create your first curriculum"
    private val manageOnlyText = "No curricula yet"

    @Before
    fun setUp() {
        val container = AppContainer(ApplicationProvider.getApplicationContext<Context>())
        compose.setContent { LearningmgmtTheme { AppNavHost(container) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(homeOnlyText).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.autoAdvance = false
    }

    private fun clickManageCurricula() = compose.onAllNodesWithText("Manage curricula")[0].performClick()

    private fun oneFrameLater() = compose.mainClock.advanceTimeBy(50)

    @Test
    fun homeIsGoneOneFrameAfterOpeningManageCurricula() {
        clickManageCurricula()
        oneFrameLater()

        compose.onAllNodesWithText(homeOnlyText).assertCountEquals(0)
        compose.onAllNodesWithText(manageOnlyText).assertCountEquals(1)
    }

    @Test
    fun manageCurriculaIsGoneOneFrameAfterGoingBack() {
        clickManageCurricula()
        compose.mainClock.advanceTimeBy(1_000)

        compose.onNodeWithContentDescription("Back").performClick()
        oneFrameLater()

        compose.onAllNodesWithText(manageOnlyText).assertCountEquals(0)
        compose.onAllNodesWithText(homeOnlyText).assertCountEquals(1)
    }
}
