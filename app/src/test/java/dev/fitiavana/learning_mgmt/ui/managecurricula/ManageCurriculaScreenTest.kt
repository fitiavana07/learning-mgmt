package dev.fitiavana.learning_mgmt.ui.managecurricula

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.fitiavana.learning_mgmt.features.backup.BackupCounts
import dev.fitiavana.learning_mgmt.features.backup.BackupSummary
import dev.fitiavana.learning_mgmt.features.backup.RestoreResult
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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

    private var backups = 0
    private var syncs = 0
    private var restores = 0
    private var confirmedRestores = 0
    private var dismissedRestores = 0

    private fun show(
        rows: List<CurriculumRow> = this.rows,
        busy: Boolean = false,
        pendingRestore: PendingRestore? = null,
        messages: Flow<BackupMessage> = emptyFlow(),
    ) {
        compose.setContent {
            LearningmgmtTheme {
                ManageCurriculaScreen(
                    rows = rows,
                    busy = busy,
                    pendingRestore = pendingRestore,
                    messages = messages,
                    onCreate = {},
                    onRename = { _, _ -> },
                    onDelete = { deleted += it },
                    onCurriculumClick = { opened += it },
                    onBackup = { backups++ },
                    onSync = { syncs++ },
                    onRestore = { restores++ },
                    onConfirmRestore = { confirmedRestores++ },
                    onDismissRestore = { dismissedRestores++ },
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

    // Backup and restore

    private fun openTopBarMenu() = compose.onNodeWithContentDescription("Backup, restore and sync").performClick()

    private val pending = PendingRestore(
        json = "{}",
        summary = BackupSummary(curricula = 3, phases = 14, topics = 52, exportedAt = "2026-10-06T18:30:00Z"),
        current = BackupCounts(curricula = 1, phases = 1, topics = 0),
    )

    private fun waitForText(text: String) = compose.waitUntil {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun topBarMenuOffersBackUpAndRestore() {
        show()

        openTopBarMenu()

        compose.onNodeWithText("Back up…").assertIsDisplayed()
        compose.onNodeWithText("Restore…").assertIsDisplayed()
    }

    @Test
    fun topBarMenuOffersSync() {
        show()

        openTopBarMenu()

        compose.onNodeWithText("Sync with other devices").assertIsDisplayed()
    }

    @Test
    fun choosingSyncOpensTheSyncScreen() {
        show()

        openTopBarMenu()
        compose.onNodeWithText("Sync with other devices").performClick()

        assertEquals(1, syncs)
        assertEquals(0, backups)
    }

    @Test
    fun syncStaysAvailableWhileABackupIsRunning() {
        show(busy = true)

        openTopBarMenu()

        compose.onNodeWithText("Sync with other devices").assertIsEnabled()
    }

    @Test
    fun choosingBackUpIsReported() {
        show()

        openTopBarMenu()
        compose.onNodeWithText("Back up…").performClick()

        assertEquals(1, backups)
        assertEquals(0, restores)
    }

    @Test
    fun choosingRestoreIsReported() {
        show()

        openTopBarMenu()
        compose.onNodeWithText("Restore…").performClick()

        assertEquals(1, restores)
        assertEquals(0, backups)
    }

    @Test
    fun menuItemsAreDisabledAndProgressIsShownWhileBusy() {
        show(busy = true)

        compose.onNodeWithContentDescription("Working…").assertIsDisplayed()
        openTopBarMenu()

        compose.onNodeWithText("Back up…").assertIsNotEnabled()
        compose.onNodeWithText("Restore…").assertIsNotEnabled()
    }

    @Test
    fun noProgressIsShownWhenIdle() {
        show()

        compose.onNodeWithContentDescription("Working…").assertDoesNotExist()
    }

    @Test
    fun restoreConfirmationComparesTheFileWithTheCurrentData() {
        show(pendingRestore = pending)

        compose.onNodeWithText("Restore from backup?").assertIsDisplayed()
        // One text node holds all three paragraphs, hence substring matching.
        compose.onNodeWithText("Backup file: 3 curricula, 14 phases, 52 topics, exported 2026-10-06.", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Current data: 1 curriculum, 1 phase, 0 topics.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("This will erase all current data and cannot be undone.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun confirmingTheRestoreIsReported() {
        show(pendingRestore = pending)

        compose.onNode(hasText("Restore") and hasClickAction()).performClick()

        assertEquals(1, confirmedRestores)
    }

    @Test
    fun cancellingTheRestoreIsReported() {
        show(pendingRestore = pending)

        compose.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissedRestores)
        assertEquals(0, confirmedRestores)
    }

    @Test
    fun noRestoreConfirmationWithoutAPendingRestore() {
        show()

        compose.onNodeWithText("Restore from backup?").assertDoesNotExist()
    }

    @Test
    fun outcomesAreShownInASnackbar() {
        show(messages = flowOf(BackupMessage.BackupSaved))

        waitForText("Backup saved")
    }

    @Test
    fun aRejectedBackupExplainsWhy() {
        show(messages = flowOf(BackupMessage.RestoreRejected(RestoreResult.Error("The file is not valid JSON"))))

        waitForText("This file can't be restored: The file is not valid JSON")
    }

    @Test
    fun aBackupFromAnotherVersionNamesBothVersions() {
        val mismatch = RestoreResult.SchemaMismatch(backupVersion = 3, currentVersion = 5)
        show(messages = flowOf(BackupMessage.RestoreRejected(mismatch)))

        waitForText("This backup was made with a different app version (database version 3, this app uses 5).")
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
