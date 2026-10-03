package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.fitiavana.learning_mgmt.AppContainer

/** The editor wired to its ViewModel: a new phase when [phaseId] is null, otherwise that phase. */
@Composable
fun PhaseEditorRoute(
    container: AppContainer,
    curriculumId: String,
    phaseId: String?,
    onClose: () -> Unit,
    onManageTopics: () -> Unit,
) {
    val editor: PhaseEditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PhaseEditorViewModel(curriculumId, phaseId, container.phaseRepository) }
        },
    )
    val state by editor.uiState.collectAsStateWithLifecycle()
    PhaseEditorScreen(
        state = state,
        onNameChange = editor::onNameChange,
        onDescriptionChange = editor::onDescriptionChange,
        onPreviewChange = editor::setPreview,
        onSave = editor::save,
        onClose = onClose,
        onManageTopics = onManageTopics,
    )
}
