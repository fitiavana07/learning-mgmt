package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.progress.Status
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhaseRow(val id: String, val number: Int, val name: String, val status: Status)

data class ManagePhasesState(val curriculumName: String = "", val rows: List<PhaseRow> = emptyList())

class ManagePhasesViewModel(
    private val curriculumId: String,
    curricula: CurriculumRepository,
    progress: ProgressRepository,
    private val phases: PhaseRepository,
) : ViewModel() {
    val uiState: StateFlow<ManagePhasesState> =
        combine(curricula.observeAll(), progress.observe(curriculumId)) { all, withStatus ->
            ManagePhasesState(
                curriculumName = all.firstOrNull { it.id == curriculumId }?.name.orEmpty(),
                rows = withStatus.map { PhaseRow(it.phase.id, it.phase.number, it.phase.name, it.status) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManagePhasesState())

    fun delete(id: String) {
        viewModelScope.launch { phases.delete(id) }
    }

    /** Moves the phase at position [from] to position [to] (0-based, in number order). */
    fun move(from: Int, to: Int) {
        viewModelScope.launch { phases.move(curriculumId, from, to) }
    }
}
