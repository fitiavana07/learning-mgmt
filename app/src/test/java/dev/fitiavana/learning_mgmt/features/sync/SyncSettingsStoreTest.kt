package dev.fitiavana.learning_mgmt.features.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncSettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private val file by lazy { folder.newFile("sync.preferences_pb").also { it.delete() } }

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    private fun store(file: File = this.file, newId: () -> String = { "generated" }): SyncSettingsStore {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        return SyncSettingsStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }), newId)
    }

    @Test
    fun theDeviceIdIsGeneratedOnFirstUseAndThenStaysTheSame() = runBlocking {
        var counter = 0
        val store = store(newId = { "dev-${++counter}" })

        assertEquals("dev-1", store.deviceId())
        assertEquals("dev-1", store.deviceId())
    }

    @Test
    fun theDeviceIdSurvivesANewStoreOverTheSameFile() = runBlocking {
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first = SyncSettingsStore(
            PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file }),
            newId = { "first" },
        ).deviceId()
        firstScope.cancel()

        assertEquals(first, store(newId = { "second" }).deviceId())
    }

    @Test
    fun thereIsNoPassphraseInitially() = runBlocking {
        assertNull(store().passphrase.first())
    }

    @Test
    fun aPassphraseIsStoredAndCanBeCleared() = runBlocking {
        val store = store()

        store.setPassphrase("correct horse")
        assertEquals("correct horse", store.passphrase.first())

        store.setPassphrase(null)
        assertNull(store.passphrase.first())
    }

    @Test
    fun aBlankPassphraseCountsAsNone() = runBlocking {
        val store = store()
        store.setPassphrase("secret")

        store.setPassphrase("   ")

        assertNull(store.passphrase.first())
    }

    @Test
    fun autoSyncIsOnByDefaultAndCanBeSwitchedOff() = runBlocking {
        val store = store()
        assertTrue(store.autoSync.first())

        store.setAutoSync(false)

        assertEquals(false, store.autoSync.first())
    }
}
