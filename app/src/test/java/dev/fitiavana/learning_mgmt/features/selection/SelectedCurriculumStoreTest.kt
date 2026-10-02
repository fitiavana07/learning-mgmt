package dev.fitiavana.learning_mgmt.features.selection

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SelectedCurriculumStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File

    @After
    fun tearDown() = scope.cancel()

    private fun dataStore(): DataStore<Preferences> {
        if (!::file.isInitialized) file = folder.newFile("selection.preferences_pb").also { it.delete() }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @Test
    fun nothingIsSelectedInitially() = runBlocking {
        assertNull(SelectedCurriculumStore(dataStore()).selectedId.first())
    }

    @Test
    fun selectStoresTheId() = runBlocking {
        val store = SelectedCurriculumStore(dataStore())

        store.select("abc")

        assertEquals("abc", store.selectedId.first())
    }

    @Test
    fun selectNullClearsTheSelection() = runBlocking {
        val store = SelectedCurriculumStore(dataStore())
        store.select("abc")

        store.select(null)

        assertNull(store.selectedId.first())
    }

    @Test
    fun selectionSurvivesANewStoreOverTheSameFile() = runBlocking {
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        file = folder.newFile("persisted.preferences_pb").also { it.delete() }
        SelectedCurriculumStore(
            PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file }),
        ).select("abc")
        firstScope.cancel()

        assertEquals("abc", SelectedCurriculumStore(dataStore()).selectedId.first())
    }
}
