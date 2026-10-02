package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.Status

/** Accent only for in-progress; completed and not-started stay neutral. */
@Composable
fun StatusChip(status: Status, modifier: Modifier = Modifier) {
    val (label, container, content) = when (status) {
        Status.IN_PROGRESS -> Triple(
            R.string.status_in_progress,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Status.COMPLETED -> Triple(
            R.string.status_completed,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Status.NOT_STARTED -> Triple(
            R.string.status_not_started,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
