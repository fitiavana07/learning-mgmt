package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.progress.PhaseAction
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.ui.home.phases.PhaseViewState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One phase of a curriculum in manage mode: shown with its status, but with no status actions. */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagedPhaseViewModel(
    curriculumId: String,
    phaseId: String,
    progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<PhaseViewState> =
        progress.observe(curriculumId)
            .flatMapLatest { phases ->
                val phase = phases.find { it.phase.id == phaseId }
                if (phase == null) {
                    flowOf(PhaseViewState.NotFound)
                } else {
                    progress.observeTopics(phaseId).map { PhaseViewState.Loaded(phase, PhaseAction.None, it) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhaseViewState.Loading)
}
