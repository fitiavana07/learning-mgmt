package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.backup.BackupData
import dev.fitiavana.learning_mgmt.features.backup.BackupRules
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRepairTest {
    private val c1 = Curriculum("c1", "Kotlin")

    private fun phase(id: String, number: Int, curriculum: String = "c1") =
        Phase(id, curriculum, number, "Phase $id", "")

    private fun topic(id: String, phase: String, number: Int, total: Int? = null) =
        Topic(id, phase, number, "Topic $id", total, if (total != null) "pages" else null)

    private fun data(
        curricula: List<Curriculum> = listOf(c1),
        phases: List<Phase> = emptyList(),
        phaseStatuses: List<PhaseStatus> = emptyList(),
        topics: List<Topic> = emptyList(),
        topicProgress: List<TopicProgress> = emptyList(),
    ) = BackupData(3, "", null, curricula, phases, phaseStatuses, topics, topicProgress)

    private fun phaseKey(id: String) = RecordKey(SyncKind.PHASE, id)
    private fun topicKey(id: String) = RecordKey(SyncKind.TOPIC, id)

    @Test
    fun consistentDataIsLeftAlone() {
        val data = data(
            phases = listOf(phase("p1", 1), phase("p2", 2)),
            phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED), PhaseStatus("p2", Status.IN_PROGRESS)),
            topics = listOf(topic("t1", "p2", 1, total = 10)),
            topicProgress = listOf(TopicProgress("t1", Status.IN_PROGRESS, 4)),
        )

        val repaired = SyncRepair.repair(data)

        assertEquals(data, repaired.data)
        assertTrue(repaired.modified.isEmpty())
        assertTrue(repaired.removed.isEmpty())
    }

    @Test
    fun rowsWhoseParentIsGoneAreDroppedAllTheWayDown() {
        val data = data(
            phases = listOf(phase("p1", 1), phase("orphan", 2, curriculum = "gone")),
            phaseStatuses = listOf(PhaseStatus("orphan", Status.IN_PROGRESS)),
            topics = listOf(topic("t1", "orphan", 1), topic("lost", "nowhere", 1)),
            topicProgress = listOf(TopicProgress("t1", Status.IN_PROGRESS, 0), TopicProgress("ghost", Status.COMPLETED, 0)),
        )

        val repaired = SyncRepair.repair(data)

        assertEquals(listOf("p1"), repaired.data.phases.map { it.id })
        assertTrue(repaired.data.phaseStatuses.isEmpty())
        assertTrue(repaired.data.topics.isEmpty())
        assertTrue(repaired.data.topicProgress.isEmpty())
        assertEquals(
            setOf(
                phaseKey("orphan"),
                RecordKey(SyncKind.PHASE_STATUS, "orphan"),
                topicKey("t1"),
                topicKey("lost"),
                RecordKey(SyncKind.TOPIC_PROGRESS, "t1"),
                RecordKey(SyncKind.TOPIC_PROGRESS, "ghost"),
            ),
            repaired.removed,
        )
    }

    @Test
    fun progressIsClampedToZeroAndTheTotal() {
        val data = data(
            phases = listOf(phase("p1", 1)),
            phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS)),
            topics = listOf(topic("over", "p1", 1, total = 5), topic("under", "p1", 2, total = 5), topic("plain", "p1", 3)),
            topicProgress = listOf(
                TopicProgress("over", Status.COMPLETED, 9),
                TopicProgress("under", Status.NOT_STARTED, -3),
                TopicProgress("plain", Status.NOT_STARTED, -1),
            ),
        )

        val repaired = SyncRepair.repair(data)

        assertEquals(
            listOf(5, 0, 0),
            repaired.data.topicProgress.map { it.done },
        )
        assertEquals(
            setOf(
                RecordKey(SyncKind.TOPIC_PROGRESS, "over"),
                RecordKey(SyncKind.TOPIC_PROGRESS, "under"),
                RecordKey(SyncKind.TOPIC_PROGRESS, "plain"),
            ),
            repaired.modified,
        )
    }

    @Test
    fun onlyTheLowestTopicStaysInProgress() {
        val data = data(
            phases = listOf(phase("p1", 1)),
            phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS)),
            topics = listOf(topic("tb", "p1", 2), topic("ta", "p1", 2), topic("tc", "p1", 1)),
            topicProgress = listOf(
                TopicProgress("tb", Status.IN_PROGRESS, 0),
                TopicProgress("ta", Status.IN_PROGRESS, 0),
                TopicProgress("tc", Status.IN_PROGRESS, 0),
            ),
        )

        val repaired = SyncRepair.repair(data)

        val status = repaired.data.topicProgress.associate { it.topicId to it.status }
        assertEquals(Status.IN_PROGRESS, status.getValue("tc"))
        assertEquals(Status.NOT_STARTED, status.getValue("ta"))
        assertEquals(Status.NOT_STARTED, status.getValue("tb"))
        assertEquals(setOf(RecordKey(SyncKind.TOPIC_PROGRESS, "ta"), RecordKey(SyncKind.TOPIC_PROGRESS, "tb")), repaired.modified)
    }

    @Test
    fun aCompletedPhaseWithAnUnfinishedTopicIsReopened() {
        val data = data(
            phases = listOf(phase("p1", 1)),
            phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED)),
            topics = listOf(topic("t1", "p1", 1), topic("t2", "p1", 2)),
            topicProgress = listOf(TopicProgress("t1", Status.COMPLETED, 0)),
        )

        val repaired = SyncRepair.repair(data)

        assertEquals(listOf(PhaseStatus("p1", Status.IN_PROGRESS)), repaired.data.phaseStatuses)
        assertEquals(setOf(RecordKey(SyncKind.PHASE_STATUS, "p1")), repaired.modified)
    }

    @Test
    fun aCompletedPhaseWithoutTopicsIsFine() {
        val data = data(phases = listOf(phase("p1", 1)), phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED)))

        assertEquals(data, SyncRepair.repair(data).data)
    }

    @Test
    fun onlyTheLowestPhaseStaysInProgress() {
        val data = data(
            phases = listOf(phase("pb", 2), phase("pa", 1), phase("pc", 2)),
            phaseStatuses = listOf(
                PhaseStatus("pb", Status.IN_PROGRESS),
                PhaseStatus("pa", Status.IN_PROGRESS),
                PhaseStatus("pc", Status.IN_PROGRESS),
            ),
        )

        val repaired = SyncRepair.repair(data)

        val status = repaired.data.phaseStatuses.associate { it.phaseId to it.status }
        assertEquals(Status.IN_PROGRESS, status.getValue("pa"))
        assertEquals(Status.NOT_STARTED, status.getValue("pb"))
        assertEquals(Status.NOT_STARTED, status.getValue("pc"))
    }

    @Test
    fun phasesOfDifferentCurriculaDoNotCompete() {
        val data = data(
            curricula = listOf(c1, Curriculum("c2", "Rust")),
            phases = listOf(phase("p1", 1), phase("p2", 1, curriculum = "c2")),
            phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS), PhaseStatus("p2", Status.IN_PROGRESS)),
        )

        assertEquals(data, SyncRepair.repair(data).data)
    }

    @Test
    fun reopeningACompletedPhaseCanDemoteAHigherPhaseInProgress() {
        val data = data(
            phases = listOf(phase("p1", 1), phase("p2", 2)),
            phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED), PhaseStatus("p2", Status.IN_PROGRESS)),
            topics = listOf(topic("t1", "p1", 1)),
        )

        val repaired = SyncRepair.repair(data)

        val status = repaired.data.phaseStatuses.associate { it.phaseId to it.status }
        assertEquals(Status.IN_PROGRESS, status.getValue("p1"))
        assertEquals(Status.NOT_STARTED, status.getValue("p2"))
    }

    @Test
    fun repairingIsIdempotentAndYieldsConsistentData() {
        val messy = data(
            phases = listOf(phase("p1", 1), phase("p2", 1), phase("orphan", 3, "gone")),
            phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED), PhaseStatus("p2", Status.IN_PROGRESS)),
            topics = listOf(topic("t1", "p1", 1, total = 3), topic("t2", "p1", 2), topic("t3", "p2", 1)),
            topicProgress = listOf(
                TopicProgress("t1", Status.IN_PROGRESS, 8),
                TopicProgress("t2", Status.IN_PROGRESS, 0),
            ),
        )

        val once = SyncRepair.repair(messy)
        val twice = SyncRepair.repair(once.data)

        assertNull(BackupRules.violation(once.data))
        assertEquals(once.data, twice.data)
        assertTrue(twice.modified.isEmpty() && twice.removed.isEmpty())
    }
}
