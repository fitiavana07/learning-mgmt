package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChangeTrackerTest {
    private lateinit var db: AppDatabase
    private lateinit var tracker: ChangeTracker
    private var now = 1_000L

    private val meta get() = runBlocking { db.syncMetaDao().all().associate { it.key() to it.meta() } }

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        tracker = ChangeTracker(db.syncMetaDao(), StampClock("dev", { now }))
    }

    @After
    fun tearDown() = db.close()

    private fun key(kind: SyncKind, id: String) = RecordKey(kind, id)

    /** c1 > p1 (status, topics t1 with progress, t2 without), plus an unrelated c2 > p2 > t3. */
    private suspend fun seed() {
        db.curriculumDao().insert(Curriculum("c1", "Kotlin", 1))
        db.curriculumDao().insert(Curriculum("c2", "Rust", 2))
        db.phaseDao().insert(Phase("p1", "c1", 1, "Basics", ""))
        db.phaseDao().insert(Phase("p2", "c2", 1, "Basics", ""))
        db.topicDao().insert(Topic("t1", "p1", 1, "One", null, null))
        db.topicDao().insert(Topic("t2", "p1", 2, "Two", null, null))
        db.topicDao().insert(Topic("t3", "p2", 1, "Three", null, null))
        db.phaseStatusDao().upsert(PhaseStatus("p1", Status.IN_PROGRESS))
        db.topicProgressDao().upsert(TopicProgress("t1", Status.IN_PROGRESS, 0))
    }

    @Test
    fun touchingARecordStoresItLiveWithAFreshStamp() = runBlocking {
        tracker.touch(SyncKind.PHASE, "p1")

        assertEquals(mapOf(key(SyncKind.PHASE, "p1") to Meta(Stamp(1_000, "dev"), deleted = false)), meta)
    }

    @Test
    fun touchingAgainGivesAHigherStampEvenIfTheClockStoodStill() = runBlocking {
        tracker.touch(SyncKind.PHASE, "p1")
        val first = meta.getValue(key(SyncKind.PHASE, "p1")).stamp

        tracker.touch(SyncKind.PHASE, "p1")

        assertTrue(meta.getValue(key(SyncKind.PHASE, "p1")).stamp > first)
    }

    @Test
    fun touchingSeveralIdsStampsThemAll() = runBlocking {
        tracker.touch(SyncKind.TOPIC, listOf("t1", "t2"))

        assertEquals(setOf(key(SyncKind.TOPIC, "t1"), key(SyncKind.TOPIC, "t2")), meta.keys)
        assertTrue(meta.values.none { it.deleted })
    }

    @Test
    fun touchingNothingChangesNothing() = runBlocking {
        tracker.touch(SyncKind.TOPIC, emptyList())

        assertTrue(meta.isEmpty())
    }

    @Test
    fun theClockContinuesAfterTheHighestStampAlreadyStored() = runBlocking {
        db.syncMetaDao().upsert(listOf(SyncMetaRow.of(key(SyncKind.TOPIC, "old"), Meta(Stamp(5_000, "other"), false))))

        tracker.touch(SyncKind.PHASE, "p1")

        assertEquals(Stamp(5_001, "dev"), meta.getValue(key(SyncKind.PHASE, "p1")).stamp)
    }

    @Test
    fun deletingACurriculumTombstonesEverythingBelowIt() = runBlocking {
        seed()

        tracker.deletedCurriculum("c1")

        assertEquals(
            setOf(
                key(SyncKind.CURRICULUM, "c1"),
                key(SyncKind.PHASE, "p1"),
                key(SyncKind.PHASE_STATUS, "p1"),
                key(SyncKind.TOPIC, "t1"),
                key(SyncKind.TOPIC, "t2"),
                key(SyncKind.TOPIC_PROGRESS, "t1"),
            ),
            meta.keys,
        )
        assertTrue(meta.values.all { it.deleted })
    }

    @Test
    fun deletingAPhaseTombstonesItsTopicsAndProgress() = runBlocking {
        seed()

        tracker.deletedPhase("p1")

        assertEquals(
            setOf(
                key(SyncKind.PHASE, "p1"),
                key(SyncKind.PHASE_STATUS, "p1"),
                key(SyncKind.TOPIC, "t1"),
                key(SyncKind.TOPIC, "t2"),
                key(SyncKind.TOPIC_PROGRESS, "t1"),
            ),
            meta.keys,
        )
        assertTrue(meta.values.all { it.deleted })
    }

    @Test
    fun deletingATopicTombstonesItAndItsProgress() = runBlocking {
        seed()

        tracker.deletedTopic("t1")

        assertEquals(setOf(key(SyncKind.TOPIC, "t1"), key(SyncKind.TOPIC_PROGRESS, "t1")), meta.keys)
        assertTrue(meta.values.all { it.deleted })
    }

    @Test
    fun aTombstoneGetsAStampNewerThanTheLiveRecordItReplaces() = runBlocking {
        seed()
        tracker.touch(SyncKind.TOPIC, "t2")
        val live = meta.getValue(key(SyncKind.TOPIC, "t2")).stamp

        tracker.deletedTopic("t2")

        assertTrue(meta.getValue(key(SyncKind.TOPIC, "t2")).stamp > live)
    }

    @Test
    fun deletedKeysAreTombstonedAsGiven() = runBlocking {
        tracker.deleted(listOf(key(SyncKind.PHASE, "gone")))

        assertEquals(true, meta.getValue(key(SyncKind.PHASE, "gone")).deleted)
    }

    @Test
    fun everyLocalWriteIsAnnounced() = runBlocking {
        val announced = async(start = CoroutineStart.UNDISPATCHED) { tracker.changes.first() }

        tracker.touch(SyncKind.PHASE, "p1")

        withTimeout(2_000) { announced.await() }
        Unit
    }
}
