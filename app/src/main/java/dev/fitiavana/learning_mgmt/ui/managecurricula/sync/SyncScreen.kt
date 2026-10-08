package dev.fitiavana.learning_mgmt.ui.managecurricula.sync

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.sync.PeerState
import dev.fitiavana.learning_mgmt.features.sync.PeerSync
import dev.fitiavana.learning_mgmt.features.sync.SessionFailure
import dev.fitiavana.learning_mgmt.features.sync.SyncState
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    state: SyncState,
    onSavePassphrase: (String) -> Boolean,
    onRemovePassphrase: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSyncWith: (String) -> Unit,
    onSyncAll: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmRemove by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = state.hasPassphrase) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.sync_refresh))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            if (state.deviceName.isNotEmpty()) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.sync_this_device)) },
                        supportingContent = { Text(state.deviceName) },
                    )
                }
            }
            item { PassphraseSection(state.hasPassphrase, onSavePassphrase, onRemove = { confirmRemove = true }) }
            if (state.hasPassphrase) {
                item { AutoSyncRow(state.autoSync, onAutoSyncChange) }
                item { DevicesHeader(hasPeers = state.peers.isNotEmpty(), onSyncAll = onSyncAll) }
                if (state.networkError != null) {
                    item { NetworkProblem(state.networkError) }
                } else if (state.peers.isEmpty()) {
                    item { NoDevices() }
                } else {
                    items(state.peers, key = { it.peer.info.deviceId }) { PeerRow(it, onClick = { onSyncWith(it.peer.info.deviceId) }) }
                }
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(R.string.sync_remove_title),
            message = stringResource(R.string.sync_remove_message),
            confirmLabel = stringResource(R.string.sync_remove_confirm),
            destructive = true,
            onConfirm = { confirmRemove = false; onRemovePassphrase() },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
private fun PassphraseSection(
    hasPassphrase: Boolean,
    onSave: (String) -> Boolean,
    onRemove: () -> Unit,
) {
    var changing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var refused by remember { mutableStateOf(false) }

    if (hasPassphrase && !changing) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.sync_passphrase_label)) },
            supportingContent = { Text(stringResource(R.string.sync_passphrase_set)) },
            trailingContent = {
                TextButton(onClick = { changing = true }) { Text(stringResource(R.string.sync_passphrase_change)) }
            },
        )
        TextButton(onClick = onRemove, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(R.string.sync_passphrase_remove), color = MaterialTheme.colorScheme.error)
        }
        return
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!hasPassphrase) {
            Text(
                stringResource(R.string.sync_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; refused = false },
            label = { Text(stringResource(R.string.sync_passphrase_label)) },
            supportingText = { Text(stringResource(R.string.sync_passphrase_hint, MIN_PASSPHRASE_LENGTH)) },
            isError = refused,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (onSave(text)) { text = ""; changing = false } else refused = true }) {
                Text(stringResource(R.string.sync_passphrase_save))
            }
            if (hasPassphrase) {
                TextButton(onClick = { changing = false; text = ""; refused = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun AutoSyncRow(autoSync: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.sync_auto)) },
        supportingContent = { Text(stringResource(R.string.sync_auto_hint)) },
        trailingContent = { Switch(checked = autoSync, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = autoSync, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun DevicesHeader(hasPeers: Boolean, onSyncAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.sync_devices),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (hasPeers) TextButton(onClick = onSyncAll) { Text(stringResource(R.string.sync_all)) }
    }
}

@Composable
private fun NetworkProblem(message: String) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.sync_network_error, message),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            stringResource(R.string.sync_network_error_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoDevices() {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.sync_no_devices), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.sync_no_devices_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeerRow(peer: PeerState, onClick: () -> Unit) {
    val failed = peer.sync is PeerSync.Failed
    ListItem(
        headlineContent = { Text(peer.peer.label) },
        supportingContent = {
            Text(
                peer.sync.text(),
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            if (peer.sync is PeerSync.Syncing) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.sync_sync), color = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PeerSync.text(): String = when (this) {
    PeerSync.Idle -> stringResource(R.string.sync_status_idle)
    PeerSync.Syncing -> stringResource(R.string.sync_status_syncing)
    is PeerSync.Synced -> stringResource(R.string.sync_status_synced, timeText(atMillis))
    is PeerSync.Failed -> failure.text()
}

@Composable
private fun SessionFailure.text(): String = when (this) {
    SessionFailure.NoAnswer -> stringResource(R.string.sync_failed_no_answer)
    SessionFailure.WrongPassphrase -> stringResource(R.string.sync_failed_wrong_passphrase)
    is SessionFailure.Incompatible -> stringResource(R.string.sync_failed_incompatible)
    is SessionFailure.Refused -> stringResource(R.string.sync_failed_refused, message)
    is SessionFailure.Invalid -> stringResource(R.string.sync_failed_invalid)
    is SessionFailure.Network -> stringResource(R.string.sync_failed_network)
}

@Composable
private fun timeText(millis: Long): String = DateFormat.getTimeFormat(LocalContext.current).format(Date(millis))
