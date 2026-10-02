package dev.fitiavana.learning_mgmt.ui.managecurricula

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.NameDialog

private sealed interface Dialog {
    data object Create : Dialog
    data class Rename(val row: CurriculumRow) : Dialog
    data class Delete(val row: CurriculumRow) : Dialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCurriculaScreen(
    rows: List<CurriculumRow>,
    onCreate: (String) -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onDelete: (String) -> Unit,
    onCurriculumClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_curricula)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
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
