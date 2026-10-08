package dev.fitiavana.learning_mgmt.ui.managecurricula

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.backup.RestoreResult
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.NameDialog
import kotlinx.coroutines.flow.Flow

private sealed interface Dialog {
    data object Create : Dialog
    data class Rename(val row: CurriculumRow) : Dialog
    data class Delete(val row: CurriculumRow) : Dialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCurriculaScreen(
    rows: List<CurriculumRow>,
    busy: Boolean,
    pendingRestore: PendingRestore?,
    messages: Flow<BackupMessage>,
    onCreate: (String) -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onDelete: (String) -> Unit,
    onCurriculumClick: (String) -> Unit,
    onBackup: () -> Unit,
    onSync: () -> Unit,
    onRestore: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onBack: () -> Unit,
) {
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(messages) {
        messages.collect { snackbar.showSnackbar(it.text(context)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.manage_curricula)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = { OverflowMenu(busy = busy, onBackup = onBackup, onRestore = onRestore, onSync = onSync) },
                )
                if (busy) {
                    val working = stringResource(R.string.backup_working)
                    LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = working })
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = Dialog.Create }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.curricula_new))
            }
        },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.curricula_none_yet),
                hint = stringResource(R.string.curricula_none_hint),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(rows, key = { it.id }) { row ->
                    CurriculumListItem(
                        row = row,
                        onClick = { onCurriculumClick(row.id) },
                        onRename = { dialog = Dialog.Rename(row) },
                        onDelete = { dialog = Dialog.Delete(row) },
                    )
                }
            }
        }
    }

    when (val shown = dialog) {
        null -> Unit
        Dialog.Create -> NameDialog(
            title = stringResource(R.string.curricula_new),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { onCreate(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is Dialog.Rename -> NameDialog(
            title = stringResource(R.string.curricula_rename_title),
            confirmLabel = stringResource(R.string.action_save),
            initialName = shown.row.name,
            onConfirm = { onRename(shown.row.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is Dialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.curricula_delete_title, shown.row.name),
            message = if (shown.row.phaseCount == 0) {
                stringResource(R.string.curricula_delete_no_phases)
            } else {
                pluralStringResource(R.plurals.curricula_delete_message, shown.row.phaseCount, shown.row.phaseCount)
            },
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { onDelete(shown.row.id); dialog = null },
            onDismiss = { dialog = null },
        )
    }

    pendingRestore?.let { pending ->
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_title),
            message = listOf(
                stringResource(
                    R.string.backup_restore_file,
                    countsText(pending.summary.curricula, pending.summary.phases, pending.summary.topics),
                    pending.summary.exportedAt.substringBefore('T'),
                ),
                stringResource(
                    R.string.backup_restore_current,
                    countsText(pending.current.curricula, pending.current.phases, pending.current.topics),
                ),
                stringResource(R.string.backup_restore_warning),
            ).joinToString("\n\n"),
            confirmLabel = stringResource(R.string.action_restore),
            destructive = true,
            onConfirm = onConfirmRestore,
            onDismiss = onDismissRestore,
        )
    }
}

/** Backup and restore are off while one runs; syncing is independent of them. */
@Composable
private fun OverflowMenu(busy: Boolean, onBackup: () -> Unit, onRestore: () -> Unit, onSync: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.backup_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sync_menu)) },
                onClick = { open = false; onSync() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.backup_menu_backup)) },
                enabled = !busy,
                onClick = { open = false; onBackup() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.backup_menu_restore)) },
                enabled = !busy,
                onClick = { open = false; onRestore() },
            )
        }
    }
}

@Composable
private fun countsText(curricula: Int, phases: Int, topics: Int): String = stringResource(
    R.string.backup_counts,
    pluralStringResource(R.plurals.backup_curricula_count, curricula, curricula),
    pluralStringResource(R.plurals.phase_count, phases, phases),
    pluralStringResource(R.plurals.backup_topics_count, topics, topics),
)

private fun BackupMessage.text(context: Context): String = when (this) {
    BackupMessage.BackupSaved -> context.getString(R.string.backup_saved)
    BackupMessage.BackupFailed -> context.getString(R.string.backup_failed)
    BackupMessage.FileUnreadable -> context.getString(R.string.backup_file_unreadable)
    BackupMessage.RestoreComplete -> context.getString(R.string.backup_restored)
    is BackupMessage.RestoreRejected -> when (val failure = failure) {
        is RestoreResult.Error -> context.getString(R.string.backup_restore_rejected, failure.message)
        is RestoreResult.SchemaMismatch ->
            context.getString(R.string.backup_version_mismatch, failure.backupVersion, failure.currentVersion)
    }
}

@Composable
private fun CurriculumListItem(
    row: CurriculumRow,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(row.name) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.phase_count, row.phaseCount, row.phaseCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.curricula_options, row.name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
