package dev.fitiavana.learning_mgmt.features.phases

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.SyncKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Structure edits only. Nothing here reads or writes progress. */
class PhaseRepository(
    private val db: RoomDatabase,
    private val dao: PhaseDao,
    private val tracker: ChangeTracker,
    private val newId: IdGenerator = UuidGenerator,
) {
    /** The phases in order, numbered by their place: stored numbers may have gaps or duplicates after a sync. */
    fun observe(curriculumId: String): Flow<List<Phase>> =
        dao.observeByCurriculum(curriculumId).map(PhaseOrdering::renumber)

    suspend fun add(curriculumId: String, name: String, description: String): String =
        db.withTransaction {
            val id = newId()
            val number = PhaseOrdering.nextNumber(dao.getByCurriculum(curriculumId))
            dao.insert(Phase(id, curriculumId, number, validName(name), description))
            tracker.touch(SyncKind.PHASE, id)
            id
        }

    suspend fun update(id: String, name: String, description: String) {
        val valid = validName(name)
        db.withTransaction {
            if (dao.updateDetails(id, valid, description) > 0) tracker.touch(SyncKind.PHASE, id)
        }
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            val phase = dao.get(id) ?: return@withTransaction
            tracker.deletedPhase(id)
            dao.delete(id)
            saveRenumbered(dao.getByCurriculum(phase.curriculumId), PhaseOrdering::renumber)
        }
    }

    /** Moves the phase at position [from] to position [to] (0-based, in number order). */
    suspend fun move(curriculumId: String, from: Int, to: Int) {
        db.withTransaction {
            saveRenumbered(dao.getByCurriculum(curriculumId)) { PhaseOrdering.move(it, from, to) }
        }
    }

    private suspend fun saveRenumbered(current: List<Phase>, reorder: (List<Phase>) -> List<Phase>) {
        val changed = reorder(current).filter { it !in current }
        dao.updateAll(changed)
        tracker.touch(SyncKind.PHASE, changed.map { it.id })
    }

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Phase name must not be blank" } }
}
