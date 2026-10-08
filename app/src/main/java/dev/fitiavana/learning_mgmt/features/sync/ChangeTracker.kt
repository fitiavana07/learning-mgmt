package dev.fitiavana.learning_mgmt.features.sync

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The one place that versions local writes. Repositories call it inside their transaction for
 * every row they create, change or delete, so each synced row always has a [Meta] with the stamp
 * of its latest write, and a deleted row leaves a tombstone.
 */
class ChangeTracker(
    private val dao: SyncMetaDao,
    private val clock: StampClock,
) {
    private val announced = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var seeded = false

    /** Emits after every local write, so auto-sync can push it shortly after. */
    val changes: Flow<Unit> = announced

    suspend fun touch(kind: SyncKind, id: String) = touch(kind, listOf(id))

    suspend fun touch(kind: SyncKind, ids: Collection<String>) = write(ids.map { RecordKey(kind, it) }, deleted = false)

    /** Tombstones [keys]: for rows already gone, or about to be, from their own table. */
    suspend fun deleted(keys: Collection<RecordKey>) = write(keys, deleted = true)

    /**
     * A bulk replacement of all the data (a restore): [live] becomes the whole set of live records,
     * freshly stamped, and every other live record is tombstoned.
     */
    suspend fun replaceAll(live: Set<RecordKey>) {
        deleted(dao.all().filter { !it.deleted && it.key() !in live }.map { it.key() })
        write(live, deleted = false)
    }

    // The delete helpers below must run before the row is deleted: they read what the cascade takes.

    suspend fun deletedCurriculum(id: String) {
        val phases = dao.phaseIdsOfCurriculum(id)
        val topics = dao.topicIdsOfCurriculum(id)
        deleted(
            listOf(RecordKey(SyncKind.CURRICULUM, id)) +
                phases.keys(SyncKind.PHASE) + dao.phaseStatusIds(phases).keys(SyncKind.PHASE_STATUS) +
                topics.keys(SyncKind.TOPIC) + dao.topicProgressIds(topics).keys(SyncKind.TOPIC_PROGRESS),
        )
    }

    suspend fun deletedPhase(id: String) {
        val topics = dao.topicIdsOfPhase(id)
        deleted(
            listOf(RecordKey(SyncKind.PHASE, id)) + dao.phaseStatusIds(listOf(id)).keys(SyncKind.PHASE_STATUS) +
                topics.keys(SyncKind.TOPIC) + dao.topicProgressIds(topics).keys(SyncKind.TOPIC_PROGRESS),
        )
    }

    suspend fun deletedTopic(id: String) {
        deleted(listOf(RecordKey(SyncKind.TOPIC, id)) + dao.topicProgressIds(listOf(id)).keys(SyncKind.TOPIC_PROGRESS))
    }

    private suspend fun write(keys: Collection<RecordKey>, deleted: Boolean) {
        if (keys.isEmpty()) return
        if (!seeded) {
            clock.observe(Stamp(dao.highestTime(), ""))
            seeded = true
        }
        dao.upsert(keys.map { SyncMetaRow.of(it, Meta(clock.next(), deleted)) })
        announced.tryEmit(Unit)
    }

    private fun List<String>.keys(kind: SyncKind) = map { RecordKey(kind, it) }
}
