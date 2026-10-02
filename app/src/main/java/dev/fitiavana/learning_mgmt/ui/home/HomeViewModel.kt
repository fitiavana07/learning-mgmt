package dev.fitiavana.learning_mgmt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseRules
import dev.fitiavana.learning_mgmt.features.progress.PhaseWithStatus
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import dev.fitiavana.learning_mgmt.features.selection.LaunchSelection
import dev.fitiavana.learning_mgmt.features.selection.SelectedCurriculumStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeContent {
    data object Loading : HomeContent
    data object NoCurricula : HomeContent
    data object NoPhases : HomeContent
    data class ReadyToStart(val phase: Phase) : HomeContent
    data class InProgress(val phase: Phase) : HomeContent
    data object AllCompleted : HomeContent

    companion object {
        fun of(phases: List<PhaseWithStatus>): HomeContent {
            PhaseRules.current(phases)?.let { return InProgress(it.phase) }
            PhaseRules.nextToStart(phases)?.let { return ReadyToStart(it.phase) }
            return if (phases.isEmpty()) NoPhases else AllCompleted
        }
    }
}

data class HomeUiState(val curriculumName: String?, val content: HomeContent)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    curricula: CurriculumRepository,
    selection: SelectedCurriculumStore,
    private val progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(curricula.observeAll(), selection.selectedId) { all, storedId ->
            LaunchSelection.resolve(storedId, all.map { it.id })?.let { id -> all.first { it.id == id } }
        }
            .distinctUntilChanged()
            .flatMapLatest { curriculum ->
                if (curriculum == null) {
                    flowOf(HomeUiState(null, HomeContent.NoCurricula))
                } else {
                    progress.observe(curriculum.id).map { HomeUiState(curriculum.name, HomeContent.of(it)) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(null, HomeContent.Loading))

    fun startNext() {
        val content = uiState.value.content as? HomeContent.ReadyToStart ?: return
        viewModelScope.launch { progress.start(content.phase.id) }
    }

    fun completeCurrent() {
        val content = uiState.value.content as? HomeContent.InProgress ?: return
        viewModelScope.launch { progress.complete(content.phase.id) }
    }
}
