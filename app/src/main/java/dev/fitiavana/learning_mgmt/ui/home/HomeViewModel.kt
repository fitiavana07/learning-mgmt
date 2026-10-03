package dev.fitiavana.learning_mgmt.ui.home

import dev.fitiavana.learning_mgmt.ui.common.TopicHandlers
import dev.fitiavana.learning_mgmt.features.progress.TopicWithProgress
import dev.fitiavana.learning_mgmt.features.progress.TopicRules
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.phases.Phase
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

sealed interface HomeContent {
    data object Loading : HomeContent
    data object NoCurricula : HomeContent
    data object NoPhases : HomeContent
    data class ReadyToStart(val phase: Phase, val topics: List<TopicWithProgress> = emptyList()) : HomeContent {
        override fun withTopics(topics: List<TopicWithProgress>) = copy(topics = topics)
    }
    data class InProgress(val phase: Phase, val topics: List<TopicWithProgress> = emptyList()) : HomeContent {
        override fun withTopics(topics: List<TopicWithProgress>) = copy(topics = topics)
    }
    data object AllCompleted : HomeContent

    /** This content with the topics of its phase, when it has a phase. */
    fun withTopics(topics: List<TopicWithProgress>): HomeContent = this

    /** The phase this content is about, when it has one. */
    val phaseId: String?
        get() = when (this) {
            is InProgress -> phase.id
            is ReadyToStart -> phase.id
            else -> null
        }

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
    selection: CurriculumSelection,
    private val progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        selection.selected
            .flatMapLatest { curriculum ->
                if (curriculum == null) {
                    flowOf(HomeUiState(null, HomeContent.NoCurricula))
                } else {
                    progress.observe(curriculum.id).flatMapLatest { phases ->
                        val content = HomeContent.of(phases)
                        val phaseId = content.phaseId
                        if (phaseId == null) {
                            flowOf(HomeUiState(curriculum.name, content))
                        } else {
                            progress.observeTopics(phaseId).map { HomeUiState(curriculum.name, content.withTopics(it)) }
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(null, HomeContent.Loading))

    val topicHandlers = TopicHandlers.of(viewModelScope, progress)

    fun startNext() {
        val content = uiState.value.content as? HomeContent.ReadyToStart ?: return
        viewModelScope.launch { progress.start(content.phase.id) }
    }

    fun completeCurrent() {
        val content = uiState.value.content as? HomeContent.InProgress ?: return
        if (!TopicRules.summary(content.topics).allCompleted) return
        viewModelScope.launch { progress.complete(content.phase.id) }
    }
}
