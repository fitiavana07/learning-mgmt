package dev.fitiavana.learning_mgmt.ui.managecurricula.sync

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.fitiavana.learning_mgmt.features.sync.Peer
import dev.fitiavana.learning_mgmt.features.sync.PeerInfo
import dev.fitiavana.learning_mgmt.features.sync.PeerState
import dev.fitiavana.learning_mgmt.features.sync.PeerSync
import dev.fitiavana.learning_mgmt.features.sync.SessionFailure
import dev.fitiavana.learning_mgmt.features.sync.SyncState
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SyncScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val saved = mutableListOf<String>()
    private var accept = true
    private var removed = 0
    private val autoSyncChanges = mutableListOf<Boolean>()
    private var refreshed = 0
    private val syncedWith = mutableListOf<String>()
    private var syncedAll = 0
    private var back = 0

    private fun peer(id: String, name: String, sync: PeerSync = PeerSync.Idle) =
        PeerState(Peer(PeerInfo(id, name), "192.168.1.20", 4000, name), sync)

    private val withPassphrase = SyncState(running = true, hasPassphrase = true, deviceName = "Sunny Wolf")

    private fun show(state: SyncState) {
        compose.setContent {
            LearningmgmtTheme {
                SyncScreen(
                    state = state,
                    onSavePassphrase = { saved += it; accept },
                    onRemovePassphrase = { removed++ },
                    onAutoSyncChange = { autoSyncChanges += it },
                    onRefresh = { refreshed++ },
                    onSyncWith = { syncedWith += it },
                    onSyncAll = { syncedAll++ },
                    onBack = { back++ },
                )
            }
        }
    }

    @Test
    fun withoutAPassphraseTheFormIsShownAndNothingElse() {
        show(SyncState(running = true, deviceName = "Sunny Wolf"))

        compose.onNodeWithText("Save passphrase").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        compose.onNodeWithText("Sunny Wolf").assertIsDisplayed()
        compose.onAllNodesWithText("Devices").assertCountEquals(0)
        compose.onAllNodesWithText("Sync automatically").assertCountEquals(0)
    }

    @Test
    fun aValidPassphraseIsHandedOver() {
        show(SyncState(running = true))

        compose.onNode(hasSetTextAction()).performTextInput("correct horse")
        compose.onNodeWithText("Save passphrase").performClick()

        assertEquals(listOf("correct horse"), saved)
    }

    @Test
    fun aRefusedPassphraseShowsWhyAndKeepsTheText() {
        accept = false
        show(SyncState(running = true))

        compose.onNode(hasSetTextAction()).performTextInput("abc")
        compose.onNodeWithText("Save passphrase").performClick()

        assertEquals(listOf("abc"), saved)
        compose.onNodeWithText("At least 6 characters", substring = true).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
    }

    @Test
    fun withAPassphraseTheDeviceAutoSyncAndAnEmptyListAreShown() {
        show(withPassphrase)

        compose.onNodeWithText("Sunny Wolf").assertIsDisplayed()
        compose.onNodeWithText("Sync automatically").assertIsDisplayed()
        compose.onNodeWithText("No other devices found").assertIsDisplayed()
        compose.onAllNodesWithText("Save passphrase").assertCountEquals(0)
    }

    @Test
    fun theAutoSyncSwitchShowsTheStateAndReportsTheChange() {
        show(withPassphrase.copy(autoSync = true))

        compose.onNodeWithText("Sync automatically").performClick()

        assertEquals(listOf(false), autoSyncChanges)
    }

    @Test
    fun turningAutoSyncOnIsReportedToo() {
        show(withPassphrase.copy(autoSync = false))

        compose.onNodeWithText("Sync automatically").performClick()

        assertEquals(listOf(true), autoSyncChanges)
    }

    @Test
    fun everyPeerIsListedWithItsStatus() {
        show(
            withPassphrase.copy(
                peers = listOf(
                    peer("1", "Agile Ant"),
                    peer("2", "Brave Otter", PeerSync.Syncing),
                    peer("3", "Calm Cat", PeerSync.Synced(atMillis = 1_790_000_000_000, changed = true)),
                    peer("4", "Daring Dog", PeerSync.Failed(SessionFailure.WrongPassphrase, 1_790_000_000_000)),
                ),
            ),
        )

        compose.onNodeWithText("Agile Ant").assertIsDisplayed()
        compose.onNodeWithText("Not synced yet").assertIsDisplayed()
        compose.onNodeWithText("Syncing…").assertIsDisplayed()
        compose.onNodeWithText("Synced at", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Wrong passphrase", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("No other devices found").assertCountEquals(0)
    }

    @Test
    fun eachKindOfFailureHasAnExplanation() {
        val failures = listOf(
            SessionFailure.NoAnswer to "same passphrase",
            SessionFailure.Incompatible("protocol 2") to "incompatible version",
            SessionFailure.Refused("database version 2, this one 3") to "database version 2, this one 3",
            SessionFailure.Invalid("bad") to "invalid data",
            SessionFailure.Network("timeout") to "Could not connect",
        )
        show(
            withPassphrase.copy(
                peers = failures.mapIndexed { i, (failure, _) -> peer("id$i", "Peer $i", PeerSync.Failed(failure, 1_790_000_000_000)) },
            ),
        )

        failures.forEach { (_, text) -> compose.onNodeWithText(text, substring = true, ignoreCase = true).assertExists() }
    }

    @Test
    fun aNetworkProblemIsShownInsteadOfTheEmptyList() {
        show(withPassphrase.copy(networkError = "Address already in use"))

        compose.onNodeWithText("Sync could not start", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Address already in use", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("No other devices found").assertCountEquals(0)
    }

    @Test
    fun tappingAPeerSyncsWithIt() {
        show(withPassphrase.copy(peers = listOf(peer("dev-a", "Agile Ant"), peer("dev-b", "Brave Otter"))))

        compose.onNodeWithText("Brave Otter").performClick()

        assertEquals(listOf("dev-b"), syncedWith)
    }

    @Test
    fun syncAllIsOfferedOnlyWhenPeersAreAround() {
        show(withPassphrase)
        compose.onAllNodesWithText("Sync all").assertCountEquals(0)
    }

    @Test
    fun syncAllSyncsEveryPeer() {
        show(withPassphrase.copy(peers = listOf(peer("1", "Agile Ant"))))

        compose.onNodeWithText("Sync all").performClick()

        assertEquals(1, syncedAll)
    }

    @Test
    fun refreshAsksTheNetworkAgain() {
        show(withPassphrase)

        compose.onNodeWithContentDescription("Refresh devices").performClick()

        assertEquals(1, refreshed)
    }

    @Test
    fun refreshIsOffWithoutAPassphrase() {
        show(SyncState(running = true))

        compose.onNodeWithContentDescription("Refresh devices").assertIsNotEnabled()
    }

    @Test
    fun removingThePassphraseAsksForConfirmationFirst() {
        show(withPassphrase)

        compose.onNodeWithText("Remove passphrase").performClick()
        compose.onNodeWithText("Stop syncing?").assertIsDisplayed()
        assertEquals(0, removed)

        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, removed)

        compose.onNodeWithText("Remove passphrase").performClick()
        compose.onNodeWithText("Stop syncing").performClick()
        assertEquals(1, removed)
    }

    @Test
    fun thePassphraseCanBeChanged() {
        show(withPassphrase)

        compose.onNodeWithText("Change passphrase").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("another secret")
        compose.onNodeWithText("Save passphrase").performClick()

        assertEquals(listOf("another secret"), saved)
    }

    @Test
    fun changingThePassphraseCanBeCancelled() {
        show(withPassphrase)

        compose.onNodeWithText("Change passphrase").performClick()
        compose.onNodeWithText("Cancel").performClick()

        compose.onAllNodesWithText("Save passphrase").assertCountEquals(0)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun backGoesBack() {
        show(withPassphrase)

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }
}
