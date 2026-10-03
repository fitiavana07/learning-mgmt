package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.topiceditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.topics.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [total] and [unit] only count while the topic is [quantified]; [total] is kept as typed. */
data class TopicEditorState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val quantified: Boolean = false,
    val total: String = "",
    val unit: String = "",
    val initialName: String = "",
    val initialQuantified: Boolean = false,
    val initialTotal: String = "",
    val initialUnit: String = "",
    val nameError: Boolean = false,
    val totalError: Boolean = false,
    val saved: Boolean = false,
) {
    val dirty: Boolean
        get() = name != initialName || quantified != initialQuantified ||
            (quantified && (total != initialTotal || unit != initialUnit))
}

/** Edits an existing topic when [topicId] is given, otherwise a new topic appended to the phase. */
class TopicEditorViewModel(
    private val phaseId: String,
    private val topicId: String?,
    private val topics: TopicRepository,
) : ViewModel() {
    private val state = MutableStateFlow(TopicEditorState())
    val uiState: StateFlow<TopicEditorState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = topicId?.let { id -> topics.observe(phaseId).first().find { it.id == id } }
            state.value = when {
                topicId == null -> TopicEditorState(loading = false)
                existing == null -> TopicEditorState(loading = false, notFound = true, isNew = false)
                else -> TopicEditorState(
                    loading = false,
                    isNew = false,
                    name = existing.name,
                    quantified = existing.total != null,
                    total = existing.total?.toString().orEmpty(),
                    unit = existing.unit.orEmpty(),
                    initialName = existing.name,
                    initialQuantified = existing.total != null,
                    initialTotal = existing.total?.toString().orEmpty(),
                    initialUnit = existing.unit.orEmpty(),
                )
            }
        }
    }

    fun onNameChange(name: String) = state.update { it.copy(name = name, nameError = false) }

    fun onQuantifiedChange(quantified: Boolean) = state.update { it.copy(quantified = quantified, totalError = false) }

    fun onTotalChange(total: String) = state.update { it.copy(total = total, totalError = false) }

    fun onUnitChange(unit: String) = state.update { it.copy(unit = unit) }

    fun save() {
        val current = state.value
        if (current.loading || current.notFound || current.saved) return
        val total = if (current.quantified) current.total.trim().toIntOrNull()?.takeIf { it > 0 } else null
        val nameError = current.name.isBlank()
        val totalError = current.quantified && total == null
        if (nameError || totalError) {
            state.update { it.copy(nameError = nameError, totalError = totalError) }
            return
        }
        viewModelScope.launch {
            if (topicId == null) {
                topics.add(phaseId, current.name, total, current.unit)
            } else {
                topics.update(topicId, current.name, total, current.unit)
            }
            state.update { it.copy(saved = true) }
        }
    }
}
