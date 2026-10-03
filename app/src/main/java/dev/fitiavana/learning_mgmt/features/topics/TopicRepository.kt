package dev.fitiavana.learning_mgmt.features.topics

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import kotlinx.coroutines.flow.Flow

/** Structure edits only. Nothing here reads or writes progress. */
class TopicRepository(
    private val db: RoomDatabase,
    private val dao: TopicDao,
    private val newId: IdGenerator = UuidGenerator,
) {
    fun observe(phaseId: String): Flow<List<Topic>> = dao.observeByPhase(phaseId)

    /** A topic with a [total] is quantified (progress counted in [unit]); without one it is simple. */
    suspend fun add(phaseId: String, name: String, total: Int? = null, unit: String? = null): String =
        db.withTransaction {
            val id = newId()
            val number = TopicOrdering.nextNumber(dao.getByPhase(phaseId))
            dao.insert(Topic(id, phaseId, number, validName(name), validTotal(total), validUnit(total, unit)))
            id
        }

    suspend fun update(id: String, name: String, total: Int?, unit: String?) =
        dao.updateDetails(id, validName(name), validTotal(total), validUnit(total, unit))

    suspend fun delete(id: String) {
        db.withTransaction {
            val topic = dao.get(id) ?: return@withTransaction
            dao.delete(id)
            saveRenumbered(dao.getByPhase(topic.phaseId), TopicOrdering::renumber)
        }
    }

    /** Moves the topic at position [from] to position [to] (0-based, in number order). */
    suspend fun move(phaseId: String, from: Int, to: Int) {
        db.withTransaction {
            saveRenumbered(dao.getByPhase(phaseId)) { TopicOrdering.move(it, from, to) }
        }
    }

    private suspend fun saveRenumbered(current: List<Topic>, reorder: (List<Topic>) -> List<Topic>) {
        val changed = reorder(current).filter { it !in current }
        dao.updateAll(changed)
    }

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Topic name must not be blank" } }

    private fun validTotal(total: Int?): Int? =
        total?.also { require(it > 0) { "Topic total must be positive" } }

    /** Only quantified topics have a unit, and a blank one means none. */
    private fun validUnit(total: Int?, unit: String?): String? =
        if (total == null) null else unit?.trim()?.takeIf { it.isNotEmpty() }
}
