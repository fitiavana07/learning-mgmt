package dev.fitiavana.learning_mgmt.features.topics

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.SyncKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Structure edits only. Nothing here reads or writes progress. */
class TopicRepository(
    private val db: RoomDatabase,
    private val dao: TopicDao,
    private val tracker: ChangeTracker,
    private val newId: IdGenerator = UuidGenerator,
) {
    /** The topics in order, numbered by their place: stored numbers may have gaps or duplicates after a sync. */
    fun observe(phaseId: String): Flow<List<Topic>> = dao.observeByPhase(phaseId).map(TopicOrdering::renumber)

    /** A topic with a [total] is quantified (progress counted in [unit]); without one it is simple. */
    suspend fun add(phaseId: String, name: String, total: Int? = null, unit: String? = null): String =
        db.withTransaction {
            val id = newId()
            val number = TopicOrdering.nextNumber(dao.getByPhase(phaseId))
            dao.insert(Topic(id, phaseId, number, validName(name), validTotal(total), validUnit(total, unit)))
            tracker.touch(SyncKind.TOPIC, id)
            id
        }

    suspend fun update(id: String, name: String, total: Int?, unit: String?) {
        val validName = validName(name)
        val validTotal = validTotal(total)
        val validUnit = validUnit(total, unit)
        db.withTransaction {
            if (dao.updateDetails(id, validName, validTotal, validUnit) > 0) tracker.touch(SyncKind.TOPIC, id)
        }
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            val topic = dao.get(id) ?: return@withTransaction
            tracker.deletedTopic(id)
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
        tracker.touch(SyncKind.TOPIC, changed.map { it.id })
    }

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Topic name must not be blank" } }

    private fun validTotal(total: Int?): Int? =
        total?.also { require(it > 0) { "Topic total must be positive" } }

    /** Only quantified topics have a unit, and a blank one means none. */
    private fun validUnit(total: Int?, unit: String?): String? =
        if (total == null) null else unit?.trim()?.takeIf { it.isNotEmpty() }
}
