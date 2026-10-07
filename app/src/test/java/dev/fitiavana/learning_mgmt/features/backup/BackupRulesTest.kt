package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {
    private val valid = BackupData(
        schemaVersion = 2,
        exportedAt = "2026-10-07T10:00:00Z",
        selectedCurriculumId = "c1",
        curricula = listOf(Curriculum("c1", "Spanish"), Curriculum("c2", "Math")),
        phases = listOf(
            Phase("p1", "c1", 1, "Basics", ""),
            Phase("p2", "c1", 2, "Verbs", ""),
            Phase("p3", "c2", 1, "Algebra", ""),
        ),
        phaseStatuses = listOf(
            PhaseStatus("p1", Status.COMPLETED),
            PhaseStatus("p2", Status.IN_PROGRESS),
            PhaseStatus("p3", Status.IN_PROGRESS),
        ),
        topics = listOf(
            Topic("t1", "p1", 1, "Greetings", null, null),
            Topic("t2", "p1", 2, "Reading", 40, "pages"),
            Topic("t3", "p2", 1, "Present", null, null),
        ),
        topicProgress = listOf(
            TopicProgress("t1", Status.COMPLETED, 0),
            TopicProgress("t2", Status.COMPLETED, 40),
            TopicProgress("t3", Status.IN_PROGRESS, 0),
        ),
    )

    private fun violation(data: BackupData) = BackupRules.violation(data)

    @Test
    fun consistentDataHasNoViolation() {
        assertNull(violation(valid))
    }

    @Test
    fun emptyDataHasNoViolation() {
        assertNull(violation(valid.copy(selectedCurriculumId = null, curricula = emptyList(), phases = emptyList(), phaseStatuses = emptyList(), topics = emptyList(), topicProgress = emptyList())))
    }

    @Test
    fun selectionOfUnknownCurriculumIsAllowed() {
        // Restore falls back to the first curriculum.
        assertNull(violation(valid.copy(selectedCurriculumId = "gone")))
    }

    @Test
    fun duplicateIdsAreRejected() {
        assertNotNull(violation(valid.copy(curricula = valid.curricula + Curriculum("c1", "Again"))))
        assertNotNull(violation(valid.copy(phases = valid.phases + Phase("p1", "c1", 9, "Again", ""))))
        assertNotNull(violation(valid.copy(topics = valid.topics + Topic("t1", "p1", 9, "Again", null, null))))
        assertNotNull(violation(valid.copy(phaseStatuses = valid.phaseStatuses + PhaseStatus("p1", Status.COMPLETED))))
        assertNotNull(violation(valid.copy(topicProgress = valid.topicProgress + TopicProgress("t1", Status.COMPLETED, 0))))
    }

    @Test
    fun rowsPointingAtMissingParentsAreRejected() {
        assertNotNull(violation(valid.copy(phases = valid.phases + Phase("p9", "nope", 1, "X", ""))))
        assertNotNull(violation(valid.copy(phaseStatuses = valid.phaseStatuses + PhaseStatus("nope", Status.NOT_STARTED))))
        assertNotNull(violation(valid.copy(topics = valid.topics + Topic("t9", "nope", 1, "X", null, null))))
        assertNotNull(violation(valid.copy(topicProgress = valid.topicProgress + TopicProgress("nope", Status.NOT_STARTED, 0))))
    }

    @Test
    fun completedPhaseWithUnfinishedTopicIsRejected() {
        // p1 is completed; t2 is not.
        val data = valid.copy(topicProgress = valid.topicProgress.map {
            if (it.topicId == "t2") TopicProgress("t2", Status.IN_PROGRESS, 10) else it
        })
        assertTrue(violation(data)!!.contains("Basics"))
    }

    @Test
    fun completedPhaseWithTopicWithoutProgressRowIsRejected() {
        val data = valid.copy(topicProgress = valid.topicProgress.filter { it.topicId != "t1" })
        assertNotNull(violation(data))
    }

    @Test
    fun twoInProgressPhasesInOneCurriculumAreRejected() {
        val data = valid.copy(phaseStatuses = valid.phaseStatuses.map {
            if (it.phaseId == "p1") PhaseStatus("p1", Status.IN_PROGRESS) else it
        })
        assertNotNull(violation(data))
    }

    @Test
    fun oneInProgressPhasePerCurriculumIsFine() {
        // valid already has p2 (c1) and p3 (c2) both in progress.
        assertNull(violation(valid))
    }

    @Test
    fun twoInProgressTopicsInOnePhaseAreRejected() {
        val data = valid.copy(
            topics = valid.topics + Topic("t4", "p2", 2, "Past", null, null),
            topicProgress = valid.topicProgress + TopicProgress("t4", Status.IN_PROGRESS, 0),
        )
        assertNotNull(violation(data))
    }

    @Test
    fun doneOutsideZeroToTotalIsRejected() {
        // p1 becomes the only in-progress phase of c1, with t2 as its in-progress topic.
        fun data(done: Int) = valid.copy(
            phaseStatuses = listOf(
                PhaseStatus("p1", Status.IN_PROGRESS),
                PhaseStatus("p2", Status.NOT_STARTED),
                PhaseStatus("p3", Status.IN_PROGRESS),
            ),
            topicProgress = listOf(
                TopicProgress("t1", Status.COMPLETED, 0),
                TopicProgress("t2", Status.IN_PROGRESS, done),
                TopicProgress("t3", Status.NOT_STARTED, 0),
            ),
        )
        assertNull(violation(data(0)))
        assertNull(violation(data(39)))
        assertNotNull(violation(data(41)))
        assertNotNull(violation(data(-1)))
    }
}
