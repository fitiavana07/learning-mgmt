package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.ReorderableColumn
import dev.fitiavana.learning_mgmt.ui.common.StatusIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePhasesScreen(
    state: ManagePhasesState,
    onAdd: () -> Unit,
    onPhaseClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onBack: () -> Unit,
) {
    var toDelete by remember { mutableStateOf<PhaseRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.manage_phases))
                        Text(
                            state.curriculumName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.phases_new))
            }
        },
    ) { padding ->
        if (state.rows.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.phases_none_yet),
                hint = stringResource(R.string.phases_none_hint),
                modifier = Modifier.padding(padding),
            )
        } else {
            ReorderableColumn(
                items = state.rows,
                key = { it.id },
                onMove = onMove,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) { row, dragging, rowModifier ->
                PhaseListItem(
                    row = row,
                    dragging = dragging,
                    onClick = { onPhaseClick(row.id) },
                    onDelete = { toDelete = row },
                    modifier = rowModifier,
                )
            }
        }
    }

    toDelete?.let { row ->
        val renumbered = stringResource(R.string.phases_delete_message)
        ConfirmDialog(
            title = stringResource(R.string.phases_delete_title, row.number),
            message = if (row.status == Status.IN_PROGRESS) {
                stringResource(R.string.phases_delete_in_progress) + " " + renumbered
            } else {
                renumbered
            },
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { onDelete(row.id); toDelete = null },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun PhaseListItem(
    row: PhaseRow,
    dragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.phase_title, row.number, row.name),
                fontWeight = if (row.status == Status.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                color = if (row.status == Status.COMPLETED) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.6f),
                )
                StatusIcon(row.status)
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.phases_options, row.number))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.clickable(onClick = onClick),
    )
}
