package dev.fitiavana.learning_mgmt.features.progress

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.features.phases.PhaseDao
import dev.fitiavana.learning_mgmt.features.phases.PhaseOrdering
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.SyncKind
import dev.fitiavana.learning_mgmt.features.topics.TopicOrdering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Progress only: reads phases and topics joined with their progress, and moves them forward. */
class ProgressRepository(
    private val db: RoomDatabase,
    private val phaseDao: PhaseDao,
    private val statusDao: PhaseStatusDao,
    private val topicProgressDao: TopicProgressDao,
    private val tracker: ChangeTracker,
) {
    // Phases and topics are read in order and numbered by their place: after a sync the stored
    // numbers can have gaps or duplicates, which only the order, not the number, must show.

    fun observe(curriculumId: String): Flow<List<PhaseWithStatus>> =
        statusDao.observeRowsByCurriculum(curriculumId).map { rows -> rows.map { it.toPhaseWithStatus() }.phasesByPlace() }

    /** Phases with status of every curriculum that has any, keyed by curriculum id, in number order. */
    fun observeAll(): Flow<Map<String, List<PhaseWithStatus>>> =
        statusDao.observeAllRows().map { rows ->
            rows.map { it.toPhaseWithStatus() }.groupBy { it.phase.curriculumId }.mapValues { it.value.phasesByPlace() }
        }

    /** The topics of a phase with their progress, in number order. */
    fun observeTopics(phaseId: String): Flow<List<TopicWithProgress>> =
        topicProgressDao.observeRowsByPhase(phaseId).map { rows -> rows.map { it.toTopicWithProgress() }.topicsByPlace() }

    suspend fun start(phaseId: String) =
        transition(phaseId, Status.IN_PROGRESS, "start") { phases, target, _ -> PhaseRules.canStart(phases, target) }

    suspend fun complete(phaseId: String) =
        transition(phaseId, Status.COMPLETED, "complete", PhaseRules::canComplete)

    suspend fun startTopic(topicId: String) = topicTransition(topicId, "start") { topics, target ->
        target.copy(status = Status.IN_PROGRESS).takeIf { TopicRules.canStart(topics, target) }
    }

    /** Completing a quantified topic fills its total. */
    suspend fun completeTopic(topicId: String) = topicTransition(topicId, "complete") { topics, target ->
        target.copy(status = Status.COMPLETED, done = target.topic.total ?: 0)
            .takeIf { TopicRules.canComplete(topics, target) }
    }

    /** Records [done] on the in-progress quantified topic; reaching its total completes it. */
    suspend fun recordTopicProgress(topicId: String, done: Int) = topicTransition(topicId, "record progress on") { topics, target ->
        TopicRules.record(target, done).takeIf { target.topic.total != null && TopicRules.canComplete(topics, target) }
    }

    private suspend fun transition(
        phaseId: String,
        to: Status,
        verb: String,
        allowed: (List<PhaseWithStatus>, PhaseWithStatus, TopicSummary) -> Boolean,
    ) {
        db.withTransaction {
            val curriculumId = checkNotNull(phaseDao.get(phaseId)) { "Unknown phase $phaseId" }.curriculumId
            val phases = statusDao.getRowsByCurriculum(curriculumId).map { it.toPhaseWithStatus() }.phasesByPlace()
            val target = phases.first { it.phase.id == phaseId }
            val topics = TopicRules.summary(topicProgressDao.getRowsByPhase(phaseId).map { it.toTopicWithProgress() })
            check(allowed(phases, target, topics)) { "Phase ${target.phase.number} cannot $verb now" }
            statusDao.upsert(PhaseStatus(phaseId, to))
            tracker.touch(SyncKind.PHASE_STATUS, phaseId)
        }
    }

    /** Applies [next] to the topic if allowed (it returns null otherwise); only while its phase is in progress. */
    private suspend fun topicTransition(
        topicId: String,
        verb: String,
        next: (List<TopicWithProgress>, TopicWithProgress) -> TopicWithProgress?,
    ) {
        db.withTransaction {
            val phaseId = checkNotNull(topicProgressDao.phaseIdOf(topicId)) { "Unknown topic $topicId" }
            check(statusDao.statusOf(phaseId) == Status.IN_PROGRESS) { "Phase must be in progress to $verb a topic" }
            val topics = topicProgressDao.getRowsByPhase(phaseId).map { it.toTopicWithProgress() }.topicsByPlace()
            val target = topics.first { it.topic.id == topicId }
            val updated = checkNotNull(next(topics, target)) { "Topic ${target.topic.number} cannot $verb now" }
            topicProgressDao.upsert(TopicProgress(topicId, updated.status, updated.done))
            tracker.touch(SyncKind.TOPIC_PROGRESS, topicId)
        }
    }

    private fun List<PhaseWithStatus>.phasesByPlace(): List<PhaseWithStatus> {
        val place = PhaseOrdering.renumber(map { it.phase }).associate { it.id to it.number }
        return map { it.copy(phase = it.phase.copy(number = place.getValue(it.phase.id))) }
    }

    private fun List<TopicWithProgress>.topicsByPlace(): List<TopicWithProgress> {
        val place = TopicOrdering.renumber(map { it.topic }).associate { it.id to it.number }
        return map { it.copy(topic = it.topic.copy(number = place.getValue(it.topic.id))) }
    }
}
