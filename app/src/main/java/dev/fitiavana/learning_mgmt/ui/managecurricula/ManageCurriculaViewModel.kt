package dev.fitiavana.learning_mgmt.ui.managecurricula

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CurriculumRow(val id: String, val name: String, val phaseCount: Int)

class ManageCurriculaViewModel(
    private val curricula: CurriculumRepository,
    progress: ProgressRepository,
) : ViewModel() {
    val rows: StateFlow<List<CurriculumRow>> =
        combine(curricula.observeAll(), progress.observeAll()) { all, phasesByCurriculum ->
            all.map { CurriculumRow(it.id, it.name, phasesByCurriculum[it.id].orEmpty().size) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { curricula.create(name) }
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { curricula.rename(id, name) }
    }

    fun delete(id: String) {
        viewModelScope.launch { curricula.delete(id) }
    }
}
