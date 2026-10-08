package dev.fitiavana.learning_mgmt.ui.managecurricula.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.sync.SyncControls
import dev.fitiavana.learning_mgmt.features.sync.SyncSettingsStore
import dev.fitiavana.learning_mgmt.features.sync.SyncState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The passphrase protects the data on the local network, so a trivial one is refused. Anyone
 * listening on the Wi-Fi can test guesses against what devices broadcast, so it must not be short.
 */
const val MIN_PASSPHRASE_LENGTH = 8

class SyncViewModel(
    private val controls: SyncControls,
    private val settings: SyncSettingsStore,
) : ViewModel() {
    val state: StateFlow<SyncState> = controls.state

    /** Stores the passphrase (trimmed) and returns true, or returns false if it is too short. */
    fun savePassphrase(text: String): Boolean {
        val passphrase = text.trim()
        if (passphrase.length < MIN_PASSPHRASE_LENGTH) return false
        viewModelScope.launch { settings.setPassphrase(passphrase) }
        return true
    }

    fun removePassphrase() {
        viewModelScope.launch { settings.setPassphrase(null) }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoSync(enabled) }
    }

    fun refresh() = controls.refresh()

    fun syncWith(deviceId: String) = controls.syncWith(deviceId)

    fun syncAll() = controls.syncAll()
}
