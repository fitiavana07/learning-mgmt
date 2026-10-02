package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.StatusIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePhasesScreen(
    state: ManagePhasesState,
    onDelete: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onBack: () -> Unit,
) {
    var toDelete by remember { mutableStateOf<PhaseRow?>(null) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val rows by rememberUpdatedState(state.rows)

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
    ) { padding ->
        if (state.rows.isEmpty()) {
            EmptyState(title = stringResource(R.string.phases_none_yet), modifier = Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(state.rows, key = { it.id }) { row ->
                    val dragging = draggedId == row.id
                    PhaseListItem(
                        row = row,
                        dragging = dragging,
                        onDelete = { toDelete = row },
                        modifier = Modifier
                            .onSizeChanged { rowHeight = it.height.toFloat() }
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (dragging) dragOffset else 0f
                                shadowElevation = if (dragging) 8.dp.toPx() else 0f
                            }
                            .pointerInput(row.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedId = row.id; dragOffset = 0f },
                                    onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                                    onDragEnd = {
                                        val from = rows.indexOfFirst { it.id == row.id }
                                        val to = dragTargetIndex(from, dragOffset, rowHeight, rows.size)
                                        draggedId = null
                                        dragOffset = 0f
                                        if (from >= 0 && to != from) onMove(from, to)
                                    },
                                    onDragCancel = { draggedId = null; dragOffset = 0f },
                                )
                            },
                    )
                }
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
        modifier = modifier,
    )
}
