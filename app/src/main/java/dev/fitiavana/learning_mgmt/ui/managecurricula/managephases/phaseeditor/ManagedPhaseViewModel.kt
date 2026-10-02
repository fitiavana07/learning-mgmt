package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.ui.home.phases.PhaseViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One phase of a curriculum in manage mode: shown with its status, but with no status actions. */
class ManagedPhaseViewModel(
    curriculumId: String,
    phaseId: String,
    progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<PhaseViewState> =
        progress.observe(curriculumId)
            .map { phases ->
                phases.find { it.phase.id == phaseId }
                    ?.let { PhaseViewState.Loaded(it, PhaseAction.None) }
                    ?: PhaseViewState.NotFound
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhaseViewState.Loading)
}
