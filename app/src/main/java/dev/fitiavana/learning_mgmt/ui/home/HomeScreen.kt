package dev.fitiavana.learning_mgmt.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.PhaseActionBar
import dev.fitiavana.learning_mgmt.ui.common.PhaseContent
import dev.fitiavana.learning_mgmt.ui.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onManageCurricula: () -> Unit,
    onOpenMenu: () -> Unit,
    onShowPhases: () -> Unit,
) {
    val content = state.content

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.curriculumName ?: stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.home_open_menu))
                    }
                },
                actions = { OverflowMenu(onShowPhases) },
            )
        },
        bottomBar = {
            when (content) {
                is HomeContent.InProgress -> PhaseActionBar(content.phase, PhaseAction.Complete, onStart, onComplete)
                is HomeContent.ReadyToStart -> PhaseActionBar(content.phase, PhaseAction.Start, onStart, onComplete)
                else -> Unit
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (content) {
                HomeContent.Loading -> Unit
                HomeContent.NoCurricula -> EmptyState(
                    title = stringResource(R.string.home_no_curricula),
                    actionLabel = stringResource(R.string.manage_curricula),
                    onAction = onManageCurricula,
                )
                HomeContent.NoPhases -> EmptyState(
                    title = stringResource(R.string.phases_none_yet),
                    hint = stringResource(R.string.home_no_phases_hint),
                    actionLabel = stringResource(R.string.manage_curricula),
                    onAction = onManageCurricula,
                )
                HomeContent.AllCompleted -> EmptyState(title = stringResource(R.string.phases_all_completed))
                is HomeContent.InProgress -> PhaseContent(content.phase) { StatusChip(Status.IN_PROGRESS) }
                is HomeContent.ReadyToStart -> PhaseContent(content.phase) {
                    Text(stringResource(R.string.home_ready_to_begin), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(onShowPhases: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.phases_title)) },
            onClick = {
                expanded = false
                onShowPhases()
            },
        )
    }
}
