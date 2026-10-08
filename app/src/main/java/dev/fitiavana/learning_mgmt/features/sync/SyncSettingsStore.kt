package dev.fitiavana.learning_mgmt.features.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Device-local sync settings, kept in preferences: they are never synced to other devices. */
class SyncSettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val newId: IdGenerator = UuidGenerator,
) {
    val passphrase: Flow<String?> = dataStore.data.map { it[PASSPHRASE] }

    val autoSync: Flow<Boolean> = dataStore.data.map { it[AUTO_SYNC] ?: true }

    /** This installation's id, generated on first use and then kept for good. */
    suspend fun deviceId(): String {
        dataStore.data.first()[DEVICE_ID]?.let { return it }
        return dataStore.edit { it[DEVICE_ID] = it[DEVICE_ID] ?: newId() }[DEVICE_ID]!!
    }

    /** A blank passphrase counts as none. */
    suspend fun setPassphrase(value: String?) {
        dataStore.edit { preferences ->
            val passphrase = value?.takeIf { it.isNotBlank() }
            if (passphrase == null) preferences.remove(PASSPHRASE) else preferences[PASSPHRASE] = passphrase
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        dataStore.edit { it[AUTO_SYNC] = enabled }
    }

    private companion object {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val PASSPHRASE = stringPreferencesKey("passphrase")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
    }
}
