package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.Status

/** Check (muted) for completed, filled accent dot for in progress, hollow circle for not started. */
@Composable
fun StatusIcon(status: Status, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        Status.COMPLETED -> Icon(
            Icons.Filled.CheckCircle, stringResource(R.string.status_completed), modifier, colors.onSurfaceVariant,
        )
        Status.IN_PROGRESS -> Icon(
            Icons.Filled.Circle, stringResource(R.string.status_in_progress), modifier, colors.primary,
        )
        Status.NOT_STARTED -> Icon(
            Icons.Outlined.Circle, stringResource(R.string.status_not_started), modifier, colors.onSurfaceVariant,
        )
    }
}
