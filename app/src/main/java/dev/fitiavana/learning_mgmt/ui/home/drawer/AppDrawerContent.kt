package dev.fitiavana.learning_mgmt.ui.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress.Summary

@Composable
fun AppDrawerContent(
    items: List<DrawerItem>,
    versionName: String,
    onSelect: (String) -> Unit,
    onManageCurricula: () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.drawer_app_title, stringResource(R.string.app_name), versionName),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            )
            items.forEach { item ->
                NavigationDrawerItem(
                    label = { CurriculumEntry(item) },
                    selected = item.selected,
                    onClick = { onSelect(item.id) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.manage_curricula)) },
                selected = false,
                onClick = onManageCurricula,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun CurriculumEntry(item: DrawerItem) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = summaryText(item.progress.summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(
                progress = { item.progress.fraction },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.drawer_progress, item.progress.position, item.progress.total),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun summaryText(summary: Summary): String = when (summary) {
    Summary.NoPhases -> stringResource(R.string.phases_none_yet)
    Summary.NotStarted -> stringResource(R.string.status_not_started)
    is Summary.InProgress -> stringResource(R.string.phase_title, summary.phase.number, summary.phase.name)
    is Summary.Next -> stringResource(R.string.phase_next, summary.phase.number, summary.phase.name)
    Summary.AllCompleted -> stringResource(R.string.phases_all_completed)
}

private val CurriculumProgress.fraction: Float
    get() = if (total == 0) 0f else position.toFloat() / total
