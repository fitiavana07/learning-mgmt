package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction

/**
 * The bottom button for [phase]: complete (after a confirmation), start, or a disabled start with a
 * hint saying what must happen first. Draws nothing when there is no action.
 */
@Composable
fun PhaseActionBar(
    phase: Phase,
    action: PhaseAction,
    onStart: () -> Unit,
    onComplete: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    val startLabel = stringResource(R.string.action_start_phase, phase.number)

    when (action) {
        PhaseAction.None -> Unit
        PhaseAction.Complete -> BottomButton(stringResource(R.string.action_complete_phase), onClick = { confirming = true })
        PhaseAction.Start -> BottomButton(startLabel, onClick = onStart)
        is PhaseAction.WaitForCompletion -> BottomButton(
            label = startLabel,
            enabled = false,
            hint = stringResource(R.string.action_wait_for_completion, action.phase.number),
        )
        is PhaseAction.WaitForStart -> BottomButton(
            label = startLabel,
            enabled = false,
            hint = stringResource(R.string.action_wait_for_start, action.phase.number),
        )
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(R.string.complete_dialog_title, phase.number),
            confirmLabel = stringResource(R.string.complete_dialog_confirm),
            onConfirm = {
                confirming = false
                onComplete()
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun BottomButton(label: String, enabled: Boolean = true, hint: String? = null, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label) }
    }
}
