package dev.fitiavana.learning_mgmt.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicRules
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress

/**
 * The topics of a phase: overall progress, then each topic with its status and, for quantified
 * topics, a progress bar. With [editable], the next topic can be started, and the one in progress
 * completed (or have its progress updated). Draws nothing when there are no topics.
 */
@Composable
fun TopicsSection(
    topics: List<TopicWithProgress>,
    editable: Boolean,
    handlers: TopicHandlers,
    modifier: Modifier = Modifier,
) {
    if (topics.isEmpty()) return
    val summary = TopicRules.summary(topics)
    val nextId = TopicRules.nextToStart(topics)?.topic?.id

    Column(modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(stringResource(R.string.topics_section_title), style = MaterialTheme.typography.titleMedium)
        Text(
            pluralStringResource(R.plurals.topics_completed_summary, summary.total, summary.completed, summary.total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        LinearProgressIndicator(
            progress = { summary.fraction },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("topics-progress"),
        )
        topics.forEach { topic ->
            TopicRowItem(
                topic = topic,
                canStart = editable && topic.topic.id == nextId,
                canProgress = editable && topic.status == Status.IN_PROGRESS,
                handlers = handlers,
            )
        }
    }
}

@Composable
private fun TopicRowItem(
    topic: TopicWithProgress,
    canStart: Boolean,
    canProgress: Boolean,
    handlers: TopicHandlers,
) {
    val id = topic.topic.id
    val total = topic.topic.total
    var editing by rememberSaveable(id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusIcon(topic.status)
            Text(
                text = stringResource(R.string.topic_title, topic.topic.number, topic.topic.name),
                fontWeight = if (topic.status == Status.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                color = if (topic.status == Status.COMPLETED) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (canStart) {
                TextButton(onClick = { handlers.onStart(id) }) { Text(stringResource(R.string.action_start_topic)) }
            }
            // A quantified topic completes by reaching its total, so only a simple one has a button.
            if (canProgress && total == null) {
                TextButton(onClick = { handlers.onComplete(id) }) { Text(stringResource(R.string.action_complete_topic)) }
            }
        }
        if (total != null) {
            Row(
                Modifier.padding(start = 36.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { TopicRules.fraction(topic) },
                    modifier = Modifier.weight(1f).testTag("topic-progress-$id"),
                )
                Text(
                    text = topic.topic.unit?.let { stringResource(R.string.topic_progress_with_unit, topic.done, total, it) }
                        ?: stringResource(R.string.topic_progress, topic.done, total),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (canProgress) {
                    IconButton(onClick = { editing = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.topic_update_progress),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (editing) {
                ProgressDialog(
                    topic,
                    onRecord = {
                        editing = false
                        handlers.onRecord(id, it)
                    },
                    onDismiss = { editing = false },
                )
            }
        }
    }
}

/** Asks for the amount done on [topic]; the confirm button stays disabled until it is a number. */
@Composable
private fun ProgressDialog(topic: TopicWithProgress, onRecord: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(topic.done.toString()) }
    val total = topic.topic.total ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.topic_update_progress)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.topic_field_done)) },
                    singleLine = true,
                    keyboardOptions = TextInput.keyboardOptions.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("progress-field"),
                )
                // The most frequent action, so the one filled (primary) button of the dialog.
                FilledIconButton(onClick = { text = TopicRules.increase(text.toIntOrNull(), total).toString() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.topic_increase_progress))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { text.toIntOrNull()?.let(onRecord) }, enabled = text.toIntOrNull() != null) {
                Text(stringResource(R.string.action_update_progress))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
