package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.backup.BackupData
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress

/**
 * Restores the rules `BackupRules` checks on data that was merged row by row, where each row was
 * individually valid but the combination may not be (two devices each started a different
 * phase, a topic was added to a completed phase, a parent was deleted elsewhere...).
 *
 * The repair is a pure function of the merged data and is idempotent. The rows it changed or
 * removed are reported so the caller can stamp them as fresh local writes: otherwise two devices
 * could keep different values under the same stamp.
 */
object SyncRepair {
    data class Repaired(
        val data: BackupData,
        val modified: Set<RecordKey>,
        val removed: Set<RecordKey>,
    )

    fun repair(data: BackupData): Repaired {
        val modified = mutableSetOf<RecordKey>()
        val removed = mutableSetOf<RecordKey>()

        // 1. Rows whose parent is gone are dropped, parents first.
        val curriculumIds = data.curricula.map { it.id }.toSet()
        val phases = data.phases.keepIf(removed, SyncKind.PHASE, { it.id }) { it.curriculumId in curriculumIds }
        val phaseIds = phases.map { it.id }.toSet()
        val topics = data.topics.keepIf(removed, SyncKind.TOPIC, { it.id }) { it.phaseId in phaseIds }
        val topicsById = topics.associateBy { it.id }
        val phaseStatuses = data.phaseStatuses.keepIf(removed, SyncKind.PHASE_STATUS, { it.phaseId }) { it.phaseId in phaseIds }
        var progress = data.topicProgress.keepIf(removed, SyncKind.TOPIC_PROGRESS, { it.topicId }) { it.topicId in topicsById }

        // 2. Quantified progress stays within 0..total.
        progress = progress.map { row ->
            val max = topicsById.getValue(row.topicId).total
            val done = row.done.coerceAtLeast(0).let { if (max != null) it.coerceAtMost(max) else it }
            row.takeIf { done == it.done } ?: row.copy(done = done).also { modified += row.key() }
        }

        // 3. One topic in progress per phase: the lowest one stays.
        val topicsByPhase = topics.groupBy { it.phaseId }
        val demotedTopics = topicsByPhase.values.flatMap { phaseTopics ->
            val inProgress = phaseTopics.sortedWith(compareBy({ it.number }, { it.id }))
                .filter { statusOf(progress, it.id) == Status.IN_PROGRESS }
            inProgress.drop(1).map { it.id }
        }.toSet()
        progress = progress.map { row ->
            if (row.topicId in demotedTopics) row.copy(status = Status.NOT_STARTED).also { modified += row.key() } else row
        }

        // 4. A completed phase needs all its topics completed, otherwise it is reopened.
        var statuses = phaseStatuses.map { row ->
            val phaseTopics = topicsByPhase[row.phaseId].orEmpty()
            val unfinished = phaseTopics.any { statusOf(progress, it.id) != Status.COMPLETED }
            if (row.status == Status.COMPLETED && unfinished) {
                row.copy(status = Status.IN_PROGRESS).also { modified += row.key() }
            } else row
        }

        // 5. One phase in progress per curriculum: the lowest one stays.
        val demotedPhases = phases.groupBy { it.curriculumId }.values.flatMap { curriculumPhases ->
            val inProgress = curriculumPhases.sortedWith(compareBy({ it.number }, { it.id }))
                .filter { phase -> statuses.any { it.phaseId == phase.id && it.status == Status.IN_PROGRESS } }
            inProgress.drop(1).map { it.id }
        }.toSet()
        statuses = statuses.map { row ->
            if (row.phaseId in demotedPhases) row.copy(status = Status.NOT_STARTED).also { modified += row.key() } else row
        }

        return Repaired(
            data = data.copy(phases = phases, topics = topics, phaseStatuses = statuses, topicProgress = progress),
            modified = modified,
            removed = removed,
        )
    }

    private fun statusOf(progress: List<TopicProgress>, topicId: String): Status =
        progress.firstOrNull { it.topicId == topicId }?.status ?: Status.NOT_STARTED

    private fun PhaseStatus.key() = RecordKey(SyncKind.PHASE_STATUS, phaseId)

    private fun TopicProgress.key() = RecordKey(SyncKind.TOPIC_PROGRESS, topicId)

    /** Keeps the rows matching [keep] and records the keys of the dropped ones in [removed]. */
    private fun <T> List<T>.keepIf(
        removed: MutableSet<RecordKey>,
        kind: SyncKind,
        id: (T) -> String,
        keep: (T) -> Boolean,
    ): List<T> {
        val (kept, dropped) = partition(keep)
        dropped.mapTo(removed) { RecordKey(kind, id(it)) }
        return kept
    }
}
