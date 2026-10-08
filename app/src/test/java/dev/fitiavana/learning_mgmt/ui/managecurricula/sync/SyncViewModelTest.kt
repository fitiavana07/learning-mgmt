package dev.fitiavana.learning_mgmt.ui.managecurricula.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.sync.SyncControls
import dev.fitiavana.learning_mgmt.features.sync.SyncSettingsStore
import dev.fitiavana.learning_mgmt.features.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SyncViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    @get:Rule
    val folder = TemporaryFolder()

    private class FakeControls : SyncControls {
        override val state: StateFlow<SyncState> = MutableStateFlow(SyncState(running = true, deviceName = "Sunny Wolf"))
        val calls = mutableListOf<String>()

        override fun refresh() {
            calls += "refresh"
        }

        override fun syncWith(deviceId: String) {
            calls += "syncWith $deviceId"
        }

        override fun syncAll() {
            calls += "syncAll"
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val controls = FakeControls()
    private lateinit var settings: SyncSettingsStore
    private lateinit var viewModel: SyncViewModel

    @Before
    fun setUp() {
        val file = File(folder.root, "sync.preferences_pb")
        settings = SyncSettingsStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        viewModel = env.track(SyncViewModel(controls, settings))
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun showsTheStateOfTheCoordinator() {
        assertEquals("Sunny Wolf", viewModel.state.value.deviceName)
        assertTrue(viewModel.state.value.running)
    }

    /** The ViewModel writes in the background: wait for the stored value to become [expected]. */
    private suspend fun passphraseBecomes(expected: String?) =
        withTimeout(WAIT_MS) { settings.passphrase.first { it == expected } }

    private suspend fun autoSyncBecomes(expected: Boolean) =
        withTimeout(WAIT_MS) { settings.autoSync.first { it == expected } }

    @Test
    fun aValidPassphraseIsTrimmedAndStored() = runBlocking {
        assertTrue(viewModel.savePassphrase("  correct horse  "))

        assertEquals("correct horse", passphraseBecomes("correct horse"))
    }

    @Test
    fun aPassphraseThatIsBlankOrTooShortIsRefusedAndNothingIsStored() = runBlocking {
        assertFalse(viewModel.savePassphrase(""))
        assertFalse(viewModel.savePassphrase("     "))
        assertFalse(viewModel.savePassphrase("short"))
        assertFalse(viewModel.savePassphrase("  abc  "))

        assertNull(settings.passphrase.first())
    }

    @Test
    fun thePassphraseNeedsExactlyTheMinimumLengthToPass() = runBlocking {
        assertEquals(8, MIN_PASSPHRASE_LENGTH)
        assertFalse(viewModel.savePassphrase("a".repeat(MIN_PASSPHRASE_LENGTH - 1)))
        assertTrue(viewModel.savePassphrase("a".repeat(MIN_PASSPHRASE_LENGTH)))
    }

    @Test
    fun removingThePassphraseClearsIt() = runBlocking {
        viewModel.savePassphrase("correct horse")
        passphraseBecomes("correct horse")

        viewModel.removePassphrase()

        assertNull(passphraseBecomes(null))
    }

    @Test
    fun autoSyncCanBeSwitchedOffAndOn() = runBlocking {
        viewModel.setAutoSync(false)
        assertFalse(autoSyncBecomes(false))

        viewModel.setAutoSync(true)
        assertTrue(autoSyncBecomes(true))
    }

    @Test
    fun theActionsAreHandedToTheCoordinator() {
        viewModel.refresh()
        viewModel.syncWith("dev-b")
        viewModel.syncAll()

        assertEquals(listOf("refresh", "syncWith dev-b", "syncAll"), controls.calls)
    }

    private companion object {
        /** Generous: the whole suite runs on one busy machine and DataStore writes on another thread. */
        const val WAIT_MS = 20_000L
    }
}
