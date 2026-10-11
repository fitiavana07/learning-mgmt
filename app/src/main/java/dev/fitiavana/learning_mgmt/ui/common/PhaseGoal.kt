package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.fitiavana.learning_mgmt.R

/** The plain-text phase [goal] under a small "Goal" label; nothing when it is blank. */
@Composable
fun PhaseGoal(goal: String, modifier: Modifier = Modifier) {
    if (goal.isBlank()) return
    Column(modifier) {
        SectionLabel(stringResource(R.string.editor_goal))
        Text(goal, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The small muted label above a phase section (goal, description). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
