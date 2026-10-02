package dev.fitiavana.learning_mgmt.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.MarkdownText
import dev.fitiavana.learning_mgmt.ui.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onManageCurricula: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    var confirmingCompletion by remember { mutableStateOf(false) }
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
            )
        },
        bottomBar = {
            when (content) {
                is HomeContent.InProgress -> BottomAction(
                    label = stringResource(R.string.home_action_complete),
                    onClick = { confirmingCompletion = true },
                )
                is HomeContent.ReadyToStart -> BottomAction(
                    label = stringResource(R.string.home_action_start, content.phase.number),
                    onClick = onStart,
                )
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

    if (confirmingCompletion && content is HomeContent.InProgress) {
        ConfirmDialog(
            title = stringResource(R.string.home_complete_dialog_title, content.phase.number),
            confirmLabel = stringResource(R.string.home_complete_dialog_confirm),
            onConfirm = {
                confirmingCompletion = false
                onComplete()
            },
            onDismiss = { confirmingCompletion = false },
        )
    }
}

@Composable
private fun PhaseContent(phase: Phase, header: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        header()
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.phase_title, phase.number, phase.name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        MarkdownText(phase.description)
    }
}

@Composable
private fun BottomAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
    ) { Text(label) }
}
