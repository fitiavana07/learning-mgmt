package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Two devices, each with its own database, merging through snapshots like a real sync session. */
@RunWith(RobolectricTestRunner::class)
class SyncRepositoryTest {
    @get:Rule
    val a = TestEnvironment("a", "a-")

    @get:Rule
    val b = TestEnvironment("b", "b-")

    private data class Ids(val curriculum: String, val phase: String, val topic: String)

    /** Kotlin > Basics (in progress) > Reading, 4 of 10 pages (in progress), all made on [e]. */
    private suspend fun seed(e: TestEnvironment): Ids {
        val curriculum = e.curricula.create("Kotlin")
        val phase = e.phases.add(curriculum, "Basics", "")
        val topic = e.topics.add(phase, "Reading", total = 10, unit = "pages")
        e.progress.start(phase)
        e.progress.startTopic(topic)
        e.progress.recordTopicProgress(topic, 4)
        return Ids(curriculum, phase, topic)
    }

    /** What a session does: the initiator sends its state, the responder merges it and answers with the result. */
    private suspend fun exchange(initiator: TestEnvironment, responder: TestEnvironment) {
        responder.sync.apply(initiator.sync.snapshot())
        initiator.sync.apply(responder.sync.snapshot())
    }

    /** The snapshot with its rows in a device-independent order, to compare two devices. */
    private suspend fun normalized(e: TestEnvironment): SyncSnapshot {
        val s = e.sync.snapshot()
        return s.copy(
            data = s.data.copy(
                exportedAt = "",
                curricula = s.data.curricula.sortedBy { it.id },
                phases = s.data.phases.sortedBy { it.id },
                phaseStatuses = s.data.phaseStatuses.sortedBy { it.phaseId },
                topics = s.data.topics.sortedBy { it.id },
                topicProgress = s.data.topicProgress.sortedBy { it.topicId },
            ),
        )
    }

    private suspend fun assertConverged() = assertEquals(normalized(a), normalized(b))

    private suspend fun doneOf(e: TestEnvironment, topic: String) =
        e.db.topicProgressDao().getRowsByPhase(e.db.topicDao().get(topic)!!.phaseId).first { it.topic.id == topic }.done

    private suspend fun curriculumNames(e: TestEnvironment) = e.curricula.observeAll().first().map { it.name }

    private suspend fun phaseNames(e: TestEnvironment, curriculum: String) =
        e.phases.observe(curriculum).first().map { it.name }

    @Test
    fun anEmptySnapshotIsConsistent() = runBlocking {
        val snapshot = a.sync.snapshot()

        assertTrue(snapshot.meta.isEmpty())
        assertNull(snapshot.violation())
        assertEquals(3, snapshot.data.schemaVersion)
    }

    @Test
    fun aSnapshotCoversEveryRowWithItsStamp() = runBlocking {
        seed(a)

        val snapshot = a.sync.snapshot()

        assertNull(snapshot.violation())
        assertEquals(snapshot.data.recordKeys(), snapshot.meta.keys)
        assertEquals(5, snapshot.meta.size)
        assertEquals(null, snapshot.data.selectedCurriculumId)
    }

    @Test
    fun aRowWithoutAStampIsStampedZeroInTheSnapshot() = runBlocking {
        a.db.curriculumDao().insert(Curriculum("raw", "Raw", 1))

        val snapshot = a.sync.snapshot()

        assertEquals(Meta(Stamp(0, ""), deleted = false), snapshot.meta.getValue(RecordKey(SyncKind.CURRICULUM, "raw")))
        assertNull(snapshot.violation())
    }

    @Test
    fun anEmptyDeviceReceivesEverything() = runBlocking {
        val ids = seed(a)

        exchange(a, b)

        assertEquals(listOf("Kotlin"), curriculumNames(b))
        assertEquals(4, doneOf(b, ids.topic))
        assertEquals(Status.IN_PROGRESS, b.db.phaseStatusDao().statusOf(ids.phase))
        assertConverged()
    }

    @Test
    fun progressTickedOnOneDeviceIsThereWhenContinuingOnTheOther() = runBlocking {
        val ids = seed(a)
        exchange(a, b)

        a.now = 3_000
        a.progress.recordTopicProgress(ids.topic, 7)
        exchange(a, b)
        assertEquals(7, doneOf(b, ids.topic))

        b.now = 4_000
        b.progress.recordTopicProgress(ids.topic, 9)
        exchange(b, a)

        assertEquals(9, doneOf(a, ids.topic))
        assertConverged()
    }

    @Test
    fun aDecrementOnTheOtherDeviceWins() = runBlocking {
        val ids = seed(a)
        exchange(a, b)
        a.now = 3_000
        a.progress.recordTopicProgress(ids.topic, 7)
        exchange(a, b)

        b.now = 4_000
        b.progress.recordTopicProgress(ids.topic, 6)
        exchange(b, a)

        assertEquals(6, doneOf(a, ids.topic))
        assertEquals(6, doneOf(b, ids.topic))
    }

    @Test
    fun theLatestRenameWinsWhicheverDeviceStartsTheSync() = runBlocking {
        val ids = seed(a)
        exchange(a, b)
        a.now = 2_000
        a.curricula.rename(ids.curriculum, "From A")
        b.now = 3_000
        b.curricula.rename(ids.curriculum, "From B")

        exchange(a, b)

        assertEquals(listOf("From B"), curriculumNames(a))
        assertEquals(listOf("From B"), curriculumNames(b))
        assertConverged()
    }

    @Test
    fun theLatestRenameWinsWhenTheOtherDeviceStartsTheSync() = runBlocking {
        val ids = seed(a)
        exchange(a, b)
        a.now = 3_000
        a.curricula.rename(ids.curriculum, "From A")
        b.now = 2_000
        b.curricula.rename(ids.curriculum, "From B")

        exchange(b, a)

        assertEquals(listOf("From A"), curriculumNames(a))
        assertEquals(listOf("From A"), curriculumNames(b))
        assertConverged()
    }

    @Test
    fun aDeletionTravelsWithEverythingBelowIt() = runBlocking {
        val ids = seed(a)
        exchange(a, b)

        a.now = 2_000
        a.curricula.delete(ids.curriculum)
        exchange(a, b)

        assertEquals(emptyList<String>(), curriculumNames(b))
        assertEquals(emptyList<String>(), phaseNames(b, ids.curriculum))
        assertNull(b.db.topicDao().get(ids.topic))
        assertTrue(b.sync.snapshot().meta.values.all { it.deleted })
        assertConverged()
    }

    @Test
    fun aLaterEditBeatsAnEarlierDeletionOnTheOtherDevice() = runBlocking {
        val ids = seed(a)
        exchange(a, b)
        b.now = 2_000
        b.phases.delete(ids.phase)
        a.now = 3_000
        a.phases.update(ids.phase, "Edited", "")

        exchange(a, b)

        assertEquals(listOf("Edited"), phaseNames(a, ids.curriculum))
        assertEquals(listOf("Edited"), phaseNames(b, ids.curriculum))
        assertNull(a.db.topicDao().get(ids.topic))
        assertConverged()
    }

    @Test
    fun aReorderOnOneDeviceIsReproducedOnTheOther() = runBlocking {
        val ids = seed(a)
        a.phases.add(ids.curriculum, "Second", "")
        a.phases.add(ids.curriculum, "Third", "")
        exchange(a, b)

        a.now = 2_000
        a.phases.move(ids.curriculum, from = 0, to = 2)
        exchange(a, b)

        assertEquals(listOf("Second", "Third", "Basics"), phaseNames(b, ids.curriculum))
        assertConverged()
    }

    @Test
    fun phasesAddedAtTheSameTimeWithTheSameNumberOrderTheSameOnBothDevices() = runBlocking {
        val ids = seed(a)
        exchange(a, b)

        a.phases.add(ids.curriculum, "From A", "")
        b.phases.add(ids.curriculum, "From B", "")
        exchange(a, b)

        assertEquals(listOf("Basics", "From A", "From B"), phaseNames(a, ids.curriculum))
        assertEquals(phaseNames(a, ids.curriculum), phaseNames(b, ids.curriculum))
        assertConverged()
    }

    @Test
    fun concurrentProgressOnTheSameTopicKeepsTheLatestWrite() = runBlocking {
        val ids = seed(a)
        exchange(a, b)
        a.now = 2_000
        a.progress.recordTopicProgress(ids.topic, 6)
        b.now = 3_000
        b.progress.recordTopicProgress(ids.topic, 8)

        exchange(a, b)

        assertEquals(8, doneOf(a, ids.topic))
        assertEquals(8, doneOf(b, ids.topic))
    }

    @Test
    fun aTopicAddedToACompletedPhaseReopensItOnBothDevices() = runBlocking {
        val curriculum = a.curricula.create("Kotlin")
        val phase = a.phases.add(curriculum, "Basics", "")
        val topic = a.topics.add(phase, "Intro")
        a.progress.start(phase)
        a.progress.startTopic(topic)
        a.progress.completeTopic(topic)
        a.progress.complete(phase)
        exchange(a, b)

        b.now = 5_000
        b.topics.add(phase, "New")
        exchange(b, a)

        assertEquals(Status.IN_PROGRESS, a.db.phaseStatusDao().statusOf(phase))
        assertEquals(Status.IN_PROGRESS, b.db.phaseStatusDao().statusOf(phase))
        assertConverged()
    }

    @Test
    fun twoPhasesStartedOnDifferentDevicesLeaveOnlyTheLowestInProgress() = runBlocking {
        val curriculum = a.curricula.create("Kotlin")
        val first = a.phases.add(curriculum, "One", "")
        val second = a.phases.add(curriculum, "Two", "")
        exchange(a, b)

        a.now = 2_000
        a.progress.start(first)
        b.now = 3_000
        // Not allowed by the repository on a device that sees the first phase unstarted, so write it as an old app would.
        b.db.phaseStatusDao().upsert(PhaseStatus(second, Status.IN_PROGRESS))
        b.tracker.touch(SyncKind.PHASE_STATUS, second)

        exchange(a, b)

        assertEquals(Status.IN_PROGRESS, a.db.phaseStatusDao().statusOf(first))
        assertEquals(Status.NOT_STARTED, a.db.phaseStatusDao().statusOf(second))
        assertConverged()
    }

    @Test
    fun aPhaseAddedUnderACurriculumDeletedElsewhereIsDropped() = runBlocking {
        val curriculum = a.curricula.create("Kotlin")
        exchange(a, b)
        b.now = 2_000
        b.curricula.delete(curriculum)
        a.now = 3_000
        a.phases.add(curriculum, "Late", "")

        exchange(a, b)

        assertEquals(emptyList<String>(), curriculumNames(a))
        assertEquals(emptyList<String>(), curriculumNames(b))
        assertEquals(emptyList<String>(), phaseNames(a, curriculum))
        assertTrue(a.sync.snapshot().meta.values.all { it.deleted })
        assertConverged()
    }

    @Test
    fun progressOnATopicDeletedElsewhereIsDropped() = runBlocking {
        val ids = seed(a)
        exchange(a, b)
        a.now = 2_000
        a.topics.delete(ids.topic)
        b.now = 3_000
        b.progress.recordTopicProgress(ids.topic, 8)

        exchange(b, a)

        assertNull(a.db.topicDao().get(ids.topic))
        assertNull(b.db.topicDao().get(ids.topic))
        assertEquals(0, b.db.topicProgressDao().getRowsByPhase(ids.phase).size)
        assertConverged()
    }

    @Test
    fun applyingTheSameSnapshotAgainChangesNothing() = runBlocking {
        seed(a)
        exchange(a, b)
        val before = normalized(b)

        val result = b.sync.apply(a.sync.snapshot())

        assertEquals(ApplyResult.Applied(changed = false), result)
        assertEquals(before, normalized(b))
    }

    @Test
    fun applyingNewRowsReportsAChange() = runBlocking {
        seed(a)

        assertEquals(ApplyResult.Applied(changed = true), b.sync.apply(a.sync.snapshot()))
    }

    @Test
    fun aSnapshotWhoseRowsLackStampsIsRejectedAndChangesNothing() = runBlocking {
        seed(a)
        val broken = a.sync.snapshot().let { it.copy(meta = emptyMap()) }

        val result = b.sync.apply(broken)

        assertTrue(result is ApplyResult.Rejected)
        assertEquals(emptyList<String>(), curriculumNames(b))
    }

    @Test
    fun aSnapshotOfAnotherSchemaVersionIsRejected() = runBlocking {
        seed(a)
        val snapshot = a.sync.snapshot()
        val other = snapshot.copy(data = snapshot.data.copy(schemaVersion = 99))

        val result = b.sync.apply(other) as ApplyResult.Rejected

        assertTrue(result.message.contains("version"))
        assertEquals(emptyList<String>(), curriculumNames(b))
    }

    @Test
    fun afterApplyingLocalStampsComeAfterEveryRemoteOne() = runBlocking {
        a.now = 9_000_000
        a.curricula.create("From A")
        b.sync.apply(a.sync.snapshot())

        b.curricula.create("Later on B")

        val stamps = b.sync.snapshot().meta
        val remote = stamps.getValue(RecordKey(SyncKind.CURRICULUM, "a-c1")).stamp
        val local = stamps.getValue(RecordKey(SyncKind.CURRICULUM, "b-c1")).stamp
        assertTrue(local > remote)
    }

    @Test
    fun rowsReceivedFromAPeerAreNotAnnouncedAsLocalChanges() = runBlocking {
        seed(a)

        val announced = announcedDuring(b) { b.sync.apply(a.sync.snapshot()) }

        assertFalse(announced)
    }

    @Test
    fun aRepairIsAnnouncedBecauseItIsALocalWriteToPush() = runBlocking {
        val curriculum = a.curricula.create("Kotlin")
        val phase = a.phases.add(curriculum, "Basics", "")
        val topic = a.topics.add(phase, "Intro")
        a.progress.start(phase)
        a.progress.startTopic(topic)
        a.progress.completeTopic(topic)
        a.progress.complete(phase)
        exchange(a, b)
        b.now = 5_000
        b.topics.add(phase, "New")

        val announced = announcedDuring(a) { a.sync.apply(b.sync.snapshot()) }

        assertTrue(announced)
    }

    private suspend fun announcedDuring(e: TestEnvironment, block: suspend () -> Unit): Boolean = coroutineScope {
        var announced = false
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { e.tracker.changes.collect { announced = true } }
        block()
        yield()
        collector.cancel()
        announced
    }
}
