package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.fitiavana.learning_mgmt.R

/** Confirmation dialog; a [destructive] confirm button is shown in the error color. */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    message: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(confirmLabel, color = color)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
