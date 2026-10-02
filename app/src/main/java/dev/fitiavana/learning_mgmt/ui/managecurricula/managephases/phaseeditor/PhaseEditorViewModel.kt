package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhaseEditorState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val description: String = "",
    val initialName: String = "",
    val initialDescription: String = "",
    val preview: Boolean = false,
    val nameError: Boolean = false,
    val saved: Boolean = false,
) {
    val dirty: Boolean get() = name != initialName || description != initialDescription
}

/** Edits an existing phase when [phaseId] is given, otherwise a new phase appended to the curriculum. */
class PhaseEditorViewModel(
    private val curriculumId: String,
    private val phaseId: String?,
    private val phases: PhaseRepository,
) : ViewModel() {
    private val state = MutableStateFlow(PhaseEditorState())
    val uiState: StateFlow<PhaseEditorState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = phaseId?.let { id -> phases.observe(curriculumId).first().find { it.id == id } }
            state.value = when {
                phaseId == null -> PhaseEditorState(loading = false)
                existing == null -> PhaseEditorState(loading = false, notFound = true, isNew = false)
                else -> PhaseEditorState(
                    loading = false,
                    isNew = false,
                    name = existing.name,
                    description = existing.description,
                    initialName = existing.name,
                    initialDescription = existing.description,
                )
            }
        }
    }

    fun onNameChange(name: String) = state.update { it.copy(name = name, nameError = false) }

    fun onDescriptionChange(description: String) = state.update { it.copy(description = description) }

    fun setPreview(preview: Boolean) = state.update { it.copy(preview = preview) }

    fun save() {
        val current = state.value
        if (current.loading || current.notFound || current.saved) return
        if (current.name.isBlank()) {
            state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            if (phaseId == null) {
                phases.add(curriculumId, current.name, current.description)
            } else {
                phases.update(phaseId, current.name, current.description)
            }
            state.update { it.copy(saved = true) }
        }
    }
}
