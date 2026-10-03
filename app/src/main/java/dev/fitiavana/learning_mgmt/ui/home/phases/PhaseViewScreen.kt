package dev.fitiavana.learning_mgmt.ui.home.phases

import dev.fitiavana.learning_mgmt.ui.common.TopicsSection
import dev.fitiavana.learning_mgmt.ui.common.TopicHandlers
import dev.fitiavana.learning_mgmt.features.progress.Status
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.PhaseActionBar
import dev.fitiavana.learning_mgmt.ui.common.PhaseContent
import dev.fitiavana.learning_mgmt.ui.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseViewScreen(
    state: PhaseViewState,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    /** Manage mode: shows an edit button for the phase. Null in read mode. */
    onEdit: (() -> Unit)? = null,
    topicHandlers: TopicHandlers = TopicHandlers.None,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phases_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (onEdit != null && state is PhaseViewState.Loaded) {
                FloatingActionButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.editor_title_edit))
                }
            }
        },
        bottomBar = {
            if (state is PhaseViewState.Loaded) {
                PhaseActionBar(state.phase.phase, state.action, onStart, onComplete)
            }
        },
    ) { padding ->
        when (state) {
            PhaseViewState.Loading -> Unit
            PhaseViewState.NotFound -> EmptyState(
                title = stringResource(R.string.phase_not_found),
                modifier = Modifier.padding(padding),
            )
            is PhaseViewState.Loaded -> PhaseContent(
                state.phase.phase,
                Modifier.padding(padding),
                header = { StatusChip(state.phase.status) },
                footer = {
                    TopicsSection(
                        topics = state.topics,
                        editable = onEdit == null && state.phase.status == Status.IN_PROGRESS,
                        handlers = topicHandlers,
                    )
                },
            )
        }
    }
}
