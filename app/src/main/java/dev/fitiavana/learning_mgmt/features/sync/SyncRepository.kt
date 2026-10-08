package dev.fitiavana.learning_mgmt.features.sync

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.features.backup.BackupDao
import dev.fitiavana.learning_mgmt.features.backup.BackupData
import dev.fitiavana.learning_mgmt.features.backup.BackupRules
import kotlinx.coroutines.CancellationException

sealed interface ApplyResult {
    /** The peer's state was merged in; [changed] tells whether anything here is different now. */
    data class Applied(val changed: Boolean) : ApplyResult

    /** The peer's state was not used and nothing here changed. */
    data class Rejected(val message: String) : ApplyResult
}

/**
 * Reads this device's state as a [SyncSnapshot] and merges a peer's snapshot into it. The merge
 * is last-write-wins per row, then repaired ([SyncRepair]), checked against [BackupRules] and
 * written in one transaction, so a bad snapshot or a failure halfway leaves the data untouched.
 */
class SyncRepository(
    private val db: RoomDatabase,
    private val backupDao: BackupDao,
    private val syncDao: SyncDao,
    private val metaDao: SyncMetaDao,
    private val tracker: ChangeTracker,
    private val schemaVersion: Int,
) {
    suspend fun snapshot(): SyncSnapshot = db.withTransaction { read() }

    suspend fun apply(remote: SyncSnapshot): ApplyResult {
        if (remote.data.schemaVersion != schemaVersion) {
            return ApplyResult.Rejected(
                "The other device uses database version ${remote.data.schemaVersion}, this one $schemaVersion",
            )
        }
        remote.violation()?.let { return ApplyResult.Rejected(it) }
        return try {
            db.withTransaction { merge(remote) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApplyResult.Rejected("The data could not be merged: ${e.message ?: "unknown error"}")
        }
    }

    private suspend fun read(): SyncSnapshot {
        val data = BackupData(
            schemaVersion = schemaVersion,
            exportedAt = "",
            selectedCurriculumId = null,
            curricula = backupDao.curricula(),
            phases = backupDao.phases(),
            phaseStatuses = backupDao.phaseStatuses(),
            topics = backupDao.topics(),
            topicProgress = backupDao.topicProgress(),
        )
        val rows = data.recordKeys()
        val stored = metaDao.all().map { it.key() to it.meta() }.filter { (key, meta) -> meta.deleted || key in rows }
        // A row that never got a stamp (data from before sync existed) is older than any real write.
        val unstamped = (rows - stored.map { it.first }.toSet()).associateWith { Meta(Stamp(0, ""), deleted = false) }
        return SyncSnapshot(data, stored.toMap() + unstamped)
    }

    private suspend fun merge(remote: SyncSnapshot): ApplyResult {
        val local = read()
        val merge = SyncMerge.merge(local.meta, remote.meta)
        tracker.observe(SyncMerge.highestTime(remote.meta))

        val repaired = SyncRepair.repair(mergedRows(local.data, remote.data, merge))
        BackupRules.violation(repaired.data)?.let { error(it) }

        val stored = metaDao.all().associate { it.key() to it.meta() }
        val metaToStore = merge.meta.filter { (key, meta) -> stored[key] != meta }
        metaDao.upsert(metaToStore.map { (key, meta) -> SyncMetaRow.of(key, meta) })
        // What the repair changed is a local write that the peers have not seen: stamp it fresh.
        repaired.modified.groupBy({ it.kind }, { it.id }).forEach { (kind, ids) -> tracker.touch(kind, ids) }
        tracker.deleted(repaired.removed)

        val rowsChanged = write(local.data, repaired.data)
        return ApplyResult.Applied(
            changed = rowsChanged || metaToStore.isNotEmpty() || repaired.modified.isNotEmpty() || repaired.removed.isNotEmpty(),
        )
    }

    /** This device's rows with the records the peer won taken from the peer, and the ones it deleted dropped. */
    private fun mergedRows(local: BackupData, remote: BackupData, merge: MergeResult) = local.copy(
        curricula = merged(SyncKind.CURRICULUM, { it.id }, local.curricula, remote.curricula, merge),
        phases = merged(SyncKind.PHASE, { it.id }, local.phases, remote.phases, merge),
        topics = merged(SyncKind.TOPIC, { it.id }, local.topics, remote.topics, merge),
        phaseStatuses = merged(SyncKind.PHASE_STATUS, { it.phaseId }, local.phaseStatuses, remote.phaseStatuses, merge),
        topicProgress = merged(SyncKind.TOPIC_PROGRESS, { it.topicId }, local.topicProgress, remote.topicProgress, merge),
    )

    private fun <T> merged(
        kind: SyncKind,
        id: (T) -> String,
        local: List<T>,
        remote: List<T>,
        merge: MergeResult,
    ): List<T> {
        val taken = merge.takenFromRemote.filter { it.kind == kind }.map { it.id }.toSet()
        val remoteById = remote.associateBy(id)
        val takenAlive = taken.filter { !merge.meta.getValue(RecordKey(kind, it)).deleted }
        return local.filter { id(it) !in taken } + takenAlive.mapNotNull { remoteById[it] }
    }

    /** Writes only the rows that differ, children before parents on delete and parents first on insert. */
    private suspend fun write(before: BackupData, after: BackupData): Boolean {
        val removedCurricula = removed(before.curricula, after.curricula) { it.id }
        val removedPhases = removed(before.phases, after.phases) { it.id }
        val removedTopics = removed(before.topics, after.topics) { it.id }
        val removedStatuses = removed(before.phaseStatuses, after.phaseStatuses) { it.phaseId }
        val removedProgress = removed(before.topicProgress, after.topicProgress) { it.topicId }
        val curricula = changed(before.curricula, after.curricula)
        val phases = changed(before.phases, after.phases)
        val topics = changed(before.topics, after.topics)
        val statuses = changed(before.phaseStatuses, after.phaseStatuses)
        val progress = changed(before.topicProgress, after.topicProgress)

        removedProgress.inChunks(syncDao::deleteTopicProgress)
        removedStatuses.inChunks(syncDao::deletePhaseStatuses)
        removedTopics.inChunks(syncDao::deleteTopics)
        removedPhases.inChunks(syncDao::deletePhases)
        removedCurricula.inChunks(syncDao::deleteCurricula)
        syncDao.upsertCurricula(curricula)
        syncDao.upsertPhases(phases)
        syncDao.upsertTopics(topics)
        syncDao.upsertPhaseStatuses(statuses)
        syncDao.upsertTopicProgress(progress)

        return listOf(removedCurricula, removedPhases, removedTopics, removedStatuses, removedProgress)
            .any { it.isNotEmpty() } ||
            listOf(curricula, phases, topics, statuses, progress).any { it.isNotEmpty() }
    }

    private fun <T> changed(before: List<T>, after: List<T>): List<T> {
        val unchanged = before.toSet()
        return after.filter { it !in unchanged }
    }

    private fun <T> removed(before: List<T>, after: List<T>, id: (T) -> String): List<String> =
        (before.map(id).toSet() - after.map(id).toSet()).toList()

    /** SQLite on older Android versions allows few variables per statement. */
    private suspend fun List<String>.inChunks(delete: suspend (List<String>) -> Unit) =
        chunked(CHUNK).forEach { delete(it) }

    private companion object {
        const val CHUNK = 500
    }
}
