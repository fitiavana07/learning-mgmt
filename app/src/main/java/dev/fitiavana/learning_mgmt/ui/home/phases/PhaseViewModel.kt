package dev.fitiavana.learning_mgmt.ui.home.phases

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
    data class Loaded(val phase: PhaseWithStatus, val action: PhaseAction) : PhaseViewState
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
                    progress.observe(curriculum.id).map { phases ->
                        phases.find { it.phase.id == phaseId }
                            ?.let { PhaseViewState.Loaded(it, PhaseRules.actionFor(phases, it)) }
                            ?: PhaseViewState.NotFound
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhaseViewState.Loading)

    fun start() {
        viewModelScope.launch { progress.start(phaseId) }
    }

    fun complete() {
        viewModelScope.launch { progress.complete(phaseId) }
    }
}
