package dev.fitiavana.learning_mgmt.features.selection

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The last-selected curriculum id, kept in preferences rather than in the database. */
class SelectedCurriculumStore(private val dataStore: DataStore<Preferences>) {
    val selectedId: Flow<String?> = dataStore.data.map { it[KEY] }

    suspend fun select(id: String?) {
        dataStore.edit { preferences ->
            if (id == null) preferences.remove(KEY) else preferences[KEY] = id
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("selected_curriculum_id")
    }
}
