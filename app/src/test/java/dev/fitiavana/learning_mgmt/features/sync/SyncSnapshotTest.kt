package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.backup.BackupData
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

class SyncSnapshotTest {
    private val data = BackupData(
        schemaVersion = 3,
        exportedAt = "",
        selectedCurriculumId = null,
        curricula = listOf(Curriculum("c1", "Kotlin", 1)),
        phases = listOf(Phase("p1", "c1", 1, "Basics", "")),
        phaseStatuses = listOf(PhaseStatus("p1", Status.IN_PROGRESS)),
        topics = listOf(Topic("t1", "p1", 1, "Intro", null, null)),
        topicProgress = listOf(TopicProgress("t1", Status.IN_PROGRESS, 0)),
    )

    private val live = Meta(Stamp(5, "a"), deleted = false)
    private val allLive = data.recordKeys().associateWith { live }

    private fun snapshot(meta: Map<RecordKey, Meta> = allLive, data: BackupData = this.data) = SyncSnapshot(data, meta)

    @Test
    fun aSnapshotWhoseRowsAllHaveLiveStampsIsConsistent() {
        assertNull(snapshot().violation())
    }

    @Test
    fun tombstonesWithoutRowsAreFine() {
        val withTombstone = allLive + (RecordKey(SyncKind.PHASE, "gone") to Meta(Stamp(9, "b"), deleted = true))

        assertNull(snapshot(withTombstone).violation())
    }

    @Test
    fun aRowWithoutAStampIsAViolation() {
        val missing = allLive - RecordKey(SyncKind.TOPIC, "t1")

        assertTrue(snapshot(missing).violation()!!.contains("stamp"))
    }

    @Test
    fun aRowStampedAsDeletedIsAViolation() {
        val deleted = allLive + (RecordKey(SyncKind.PHASE, "p1") to Meta(Stamp(9, "b"), deleted = true))

        assertTrue(snapshot(deleted).violation()!!.contains("deleted"))
    }

    @Test
    fun aLiveStampWithoutARowIsAViolation() {
        val extra = allLive + (RecordKey(SyncKind.TOPIC, "ghost") to live)

        assertTrue(snapshot(extra).violation()!!.contains("no row"))
    }

    @Test
    fun rulesBetweenRowsAreLeftToTheRepairNotTheSnapshotCheck() {
        // A device can hold such a state itself (a topic added to a completed phase): the receiver repairs it.
        val twoInProgress = data.copy(
            phases = data.phases + Phase("p2", "c1", 2, "More", ""),
            phaseStatuses = data.phaseStatuses + PhaseStatus("p2", Status.IN_PROGRESS),
        )

        assertNull(snapshot(twoInProgress.recordKeys().associateWith { live }, twoInProgress).violation())
    }
}
