package dev.fitiavana.learning_mgmt.ui.home.phases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.StatusIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhasesScreen(
    state: PhasesUiState,
    onPhaseClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.phases_title))
                        state.curriculumName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
        when {
            state.loading -> Unit
            state.phases.isEmpty() -> EmptyState(
                title = stringResource(R.string.phases_none_yet),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(state.phases, key = { it.phase.id }) { item ->
                    PhaseRow(item, onClick = { onPhaseClick(item.phase.id) })
                }
            }
        }
    }
}

@Composable
private fun PhaseRow(item: PhaseWithStatus, onClick: () -> Unit) {
    val current = item.status == Status.IN_PROGRESS
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.phase_title, item.phase.number, item.phase.name),
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                color = if (item.status == Status.COMPLETED) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingContent = { StatusIcon(item.status) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
