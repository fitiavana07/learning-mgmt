package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.topiceditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.fitiavana.learning_mgmt.AppContainer

/** The editor wired to its ViewModel: a new topic when [topicId] is null, otherwise that topic. */
@Composable
fun TopicEditorRoute(container: AppContainer, phaseId: String, topicId: String?, onClose: () -> Unit) {
    val editor: TopicEditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TopicEditorViewModel(phaseId, topicId, container.topicRepository) }
        },
    )
    val state by editor.uiState.collectAsStateWithLifecycle()
    TopicEditorScreen(
        state = state,
        onNameChange = editor::onNameChange,
        onQuantifiedChange = editor::onQuantifiedChange,
        onTotalChange = editor::onTotalChange,
        onUnitChange = editor::onUnitChange,
        onSave = editor::save,
        onClose = onClose,
    )
}
