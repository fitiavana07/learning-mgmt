package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.topics.TopicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TopicRow(
    val id: String,
    val number: Int,
    val name: String,
    val status: Status,
    val total: Int?,
    val done: Int,
    val unit: String?,
)

/** [locked] when the phase is completed: its topics can then no longer be changed. */
data class ManageTopicsState(
    val phaseNumber: Int = 0,
    val phaseName: String = "",
    val rows: List<TopicRow> = emptyList(),
    val locked: Boolean = false,
)

class ManageTopicsViewModel(
    curriculumId: String,
    private val phaseId: String,
    progress: ProgressRepository,
    private val topics: TopicRepository,
) : ViewModel() {
    val uiState: StateFlow<ManageTopicsState> =
        combine(progress.observe(curriculumId), progress.observeTopics(phaseId)) { phases, withProgress ->
            val phase = phases.firstOrNull { it.phase.id == phaseId } ?: return@combine ManageTopicsState()
            ManageTopicsState(
                phaseNumber = phase.phase.number,
                phaseName = phase.phase.name,
                rows = withProgress.map {
                    TopicRow(it.topic.id, it.topic.number, it.topic.name, it.status, it.topic.total, it.done, it.topic.unit)
                },
                locked = phase.status == Status.COMPLETED,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageTopicsState())

    fun delete(id: String) {
        viewModelScope.launch { topics.delete(id) }
    }

    /** Moves the topic at position [from] to position [to] (0-based, in number order). */
    fun move(from: Int, to: Int) {
        viewModelScope.launch { topics.move(phaseId, from, to) }
    }
}
