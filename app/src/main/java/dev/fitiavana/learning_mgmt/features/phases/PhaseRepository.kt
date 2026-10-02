package dev.fitiavana.learning_mgmt.features.phases

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import kotlinx.coroutines.flow.Flow

/** Structure edits only. Nothing here reads or writes progress. */
class PhaseRepository(
    private val db: RoomDatabase,
    private val dao: PhaseDao,
    private val newId: IdGenerator = UuidGenerator,
) {
    fun observe(curriculumId: String): Flow<List<Phase>> = dao.observeByCurriculum(curriculumId)

    suspend fun add(curriculumId: String, name: String, description: String): String =
        db.withTransaction {
            val id = newId()
            val number = PhaseOrdering.nextNumber(dao.getByCurriculum(curriculumId))
            dao.insert(Phase(id, curriculumId, number, validName(name), description))
            id
        }

    suspend fun update(id: String, name: String, description: String) =
        dao.updateDetails(id, validName(name), description)

    suspend fun delete(id: String) {
        db.withTransaction {
            val phase = dao.get(id) ?: return@withTransaction
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
    }

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Phase name must not be blank" } }
}
