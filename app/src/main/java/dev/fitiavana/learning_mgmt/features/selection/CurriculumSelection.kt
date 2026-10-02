package dev.fitiavana.learning_mgmt.features.selection

import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** The curriculum the app is showing: the stored choice if it still exists, else the first one. */
class CurriculumSelection(
    curricula: CurriculumRepository,
    private val store: SelectedCurriculumStore,
) {
    val selected: Flow<Curriculum?> =
        combine(curricula.observeAll(), store.selectedId) { all, storedId ->
            LaunchSelection.resolve(storedId, all.map { it.id })?.let { id -> all.first { it.id == id } }
        }.distinctUntilChanged()

    suspend fun select(id: String) = store.select(id)
}
