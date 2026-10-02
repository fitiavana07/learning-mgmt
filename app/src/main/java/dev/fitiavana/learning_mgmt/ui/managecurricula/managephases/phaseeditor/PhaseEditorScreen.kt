package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.fitiavana.learning_mgmt.R
import dev.fitiavana.learning_mgmt.ui.common.ConfirmDialog
import dev.fitiavana.learning_mgmt.ui.common.EmptyState
import dev.fitiavana.learning_mgmt.ui.common.MarkdownText
import dev.fitiavana.learning_mgmt.ui.common.TextInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseEditorScreen(
    state: PhaseEditorState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPreviewChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestClose = { if (state.dirty) confirmDiscard = true else onClose() }

    LaunchedEffect(state.saved) { if (state.saved) onClose() }
    BackHandler(enabled = state.dirty && !state.saved, onBack = requestClose)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit))
                },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (!state.loading && !state.notFound) {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> Unit
            state.notFound -> EmptyState(
                title = stringResource(R.string.phase_not_found),
                modifier = Modifier.padding(padding),
            )
            else -> EditorForm(state, onNameChange, onDescriptionChange, onPreviewChange, Modifier.padding(padding))
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.editor_discard_title),
            message = stringResource(R.string.editor_discard_message),
            confirmLabel = stringResource(R.string.editor_discard_confirm),
            destructive = true,
            onConfirm = { confirmDiscard = false; onClose() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun EditorForm(
    state: PhaseEditorState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPreviewChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.field_name)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.editor_name_required)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = TextInput.keyboardOptions,
            modifier = Modifier.fillMaxWidth().testTag("name-field"),
        )
        WriteOrPreviewToggle(state.preview, onPreviewChange, Modifier.padding(vertical = 8.dp))
        if (state.preview) {
            MarkdownText(state.description)
        } else {
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.editor_description)) },
                minLines = 8,
                keyboardOptions = TextInput.keyboardOptions,
                modifier = Modifier.fillMaxWidth().testTag("description-field"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteOrPreviewToggle(preview: Boolean, onPreviewChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val labels = listOf(R.string.editor_write to false, R.string.editor_preview to true)
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, (label, isPreview) ->
            SegmentedButton(
                selected = preview == isPreview,
                onClick = { onPreviewChange(isPreview) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
            ) {
                Text(stringResource(label))
            }
        }
    }
}
