package dev.fitiavana.learning_mgmt.ui.home.phases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class PhasesUiState(
    val loading: Boolean,
    val curriculumName: String?,
    val phases: List<PhaseWithStatus>,
)

/** The phases of the selected curriculum, with their status. */
@OptIn(ExperimentalCoroutinesApi::class)
class PhasesViewModel(
    selection: CurriculumSelection,
    progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<PhasesUiState> =
        selection.selected
            .flatMapLatest { curriculum ->
                if (curriculum == null) {
                    flowOf(PhasesUiState(loading = false, curriculumName = null, phases = emptyList()))
                } else {
                    progress.observe(curriculum.id).map { PhasesUiState(false, curriculum.name, it) }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                PhasesUiState(loading = true, curriculumName = null, phases = emptyList()),
            )
}
