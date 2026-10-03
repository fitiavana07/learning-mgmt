package dev.fitiavana.learning_mgmt.ui.home.phases

import dev.fitiavana.learning_mgmt.ui.common.TopicHandlers
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress
import dev.fitiavana.learning_mgmt.features.progress.TopicRules
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.PhaseRules
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PhaseViewState {
    data object Loading : PhaseViewState
    data object NotFound : PhaseViewState
    data class Loaded(
        val phase: PhaseWithStatus,
        val action: PhaseAction,
        val topics: List<TopicWithProgress> = emptyList(),
    ) : PhaseViewState
}

/** One phase of the selected curriculum, read-only except for its status. */
@OptIn(ExperimentalCoroutinesApi::class)
class PhaseViewModel(
    private val phaseId: String,
    selection: CurriculumSelection,
    private val progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<PhaseViewState> =
        selection.selected
            .flatMapLatest { curriculum ->
                if (curriculum == null) {
                    flowOf(PhaseViewState.NotFound)
                } else {
                    progress.observe(curriculum.id).flatMapLatest { phases ->
                        val target = phases.find { it.phase.id == phaseId }
                        if (target == null) {
                            flowOf(PhaseViewState.NotFound)
                        } else {
                            progress.observeTopics(phaseId).map { topics ->
                                PhaseViewState.Loaded(
                                    target,
                                    PhaseRules.actionFor(phases, target, TopicRules.summary(topics)),
                                    topics,
                                )
                            }
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhaseViewState.Loading)

    val topicHandlers = TopicHandlers.of(viewModelScope, progress)

    fun start() {
        viewModelScope.launch { progress.start(phaseId) }
    }

    fun complete() {
        val loaded = uiState.value as? PhaseViewState.Loaded
        if (loaded != null && !TopicRules.summary(loaded.topics).allCompleted) return
        viewModelScope.launch { progress.complete(phaseId) }
    }
}
