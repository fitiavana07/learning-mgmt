package dev.fitiavana.learning_mgmt.ui.home.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.progress.CurriculumProgress
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.CurriculumSelection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DrawerItem(
    val id: String,
    val name: String,
    val progress: CurriculumProgress,
    val selected: Boolean,
)

class DrawerViewModel(
    curricula: CurriculumRepository,
    private val selection: CurriculumSelection,
    progress: ProgressRepository,
) : ViewModel() {
    val items: StateFlow<List<DrawerItem>> =
        combine(curricula.observeAll(), selection.selected, progress.observeAll()) { all, selected, phasesByCurriculum ->
            all.map { curriculum ->
                DrawerItem(
                    id = curriculum.id,
                    name = curriculum.name,
                    progress = CurriculumProgress.of(phasesByCurriculum[curriculum.id].orEmpty()),
                    selected = curriculum.id == selected?.id,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(id: String) {
        viewModelScope.launch { selection.select(id) }
    }
}
