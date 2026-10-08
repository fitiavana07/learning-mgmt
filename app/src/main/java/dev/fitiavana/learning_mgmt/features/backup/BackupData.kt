package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.sync.RecordKey
import dev.fitiavana.learning_mgmt.features.sync.SyncKind
import dev.fitiavana.learning_mgmt.features.topics.Topic

/**
 * Everything a backup holds: one list per table, in stored order (curricula have no order
 * column, their list order is the insertion order), plus the selected curriculum from DataStore.
 */
data class BackupData(
    val schemaVersion: Int,
    val exportedAt: String,
    val selectedCurriculumId: String?,
    val curricula: List<Curriculum>,
    val phases: List<Phase>,
    val phaseStatuses: List<PhaseStatus>,
    val topics: List<Topic>,
    val topicProgress: List<TopicProgress>,
) {
    /** The sync records this data holds: one per row, progress rows keyed by their parent id. */
    fun recordKeys(): Set<RecordKey> = buildSet {
        curricula.mapTo(this) { RecordKey(SyncKind.CURRICULUM, it.id) }
        phases.mapTo(this) { RecordKey(SyncKind.PHASE, it.id) }
        topics.mapTo(this) { RecordKey(SyncKind.TOPIC, it.id) }
        phaseStatuses.mapTo(this) { RecordKey(SyncKind.PHASE_STATUS, it.phaseId) }
        topicProgress.mapTo(this) { RecordKey(SyncKind.TOPIC_PROGRESS, it.topicId) }
    }
}
