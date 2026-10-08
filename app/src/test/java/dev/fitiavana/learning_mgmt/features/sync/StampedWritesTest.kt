package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.db.TestEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Every repository write must leave the stamp of its row behind, so a sync can tell what changed. */
@RunWith(RobolectricTestRunner::class)
class StampedWritesTest {
    @get:Rule
    val env = TestEnvironment()

    private fun meta() = runBlocking { env.db.syncMetaDao().all().associate { it.key() to it.meta() } }

    private fun stamp(kind: SyncKind, id: String) = meta().getValue(RecordKey(kind, id)).stamp

    private fun live(kind: SyncKind, id: String) = meta()[RecordKey(kind, id)]?.deleted == false

    private fun deleted(kind: SyncKind, id: String) = meta()[RecordKey(kind, id)]?.deleted == true

    @Test
    fun creatingAndRenamingACurriculumStampsIt() = runBlocking {
        val id = env.curricula.create("Kotlin")
        val created = stamp(SyncKind.CURRICULUM, id)
        assertTrue(live(SyncKind.CURRICULUM, id))

        env.curricula.rename(id, "Kotlin 2")

        assertTrue(stamp(SyncKind.CURRICULUM, id) > created)
    }

    @Test
    fun addingAndEditingPhasesAndTopicsStampsThem() = runBlocking {
        val curriculum = env.curricula.create("Kotlin")
        val phase = env.phases.add(curriculum, "Basics", "")
        val topic = env.topics.add(phase, "Intro")
        val phaseCreated = stamp(SyncKind.PHASE, phase)
        val topicCreated = stamp(SyncKind.TOPIC, topic)

        env.phases.update(phase, "Basics 2", "md")
        env.topics.update(topic, "Intro 2", total = 10, unit = "pages")

        assertTrue(stamp(SyncKind.PHASE, phase) > phaseCreated)
        assertTrue(stamp(SyncKind.TOPIC, topic) > topicCreated)
    }

    @Test
    fun deletingACurriculumTombstonesEverythingBelowIt() = runBlocking {
        val curriculum = env.curricula.create("Kotlin")
        val phase = env.phases.add(curriculum, "Basics", "")
        val topic = env.topics.add(phase, "Intro", total = 5, unit = "pages")
        env.progress.start(phase)
        env.progress.startTopic(topic)

        env.curricula.delete(curriculum)

        assertTrue(deleted(SyncKind.CURRICULUM, curriculum))
        assertTrue(deleted(SyncKind.PHASE, phase))
        assertTrue(deleted(SyncKind.PHASE_STATUS, phase))
        assertTrue(deleted(SyncKind.TOPIC, topic))
        assertTrue(deleted(SyncKind.TOPIC_PROGRESS, topic))
        assertTrue(meta().values.all { it.deleted })
    }

    @Test
    fun deletingAPhaseTombstonesItAndTouchesTheSurvivorsItRenumbers() = runBlocking {
        val curriculum = env.curricula.create("Kotlin")
        val first = env.phases.add(curriculum, "One", "")
        val second = env.phases.add(curriculum, "Two", "")
        val topic = env.topics.add(first, "Intro")
        val secondBefore = stamp(SyncKind.PHASE, second)

        env.phases.delete(first)

        assertTrue(deleted(SyncKind.PHASE, first))
        assertTrue(deleted(SyncKind.TOPIC, topic))
        assertTrue(live(SyncKind.PHASE, second))
        assertTrue(stamp(SyncKind.PHASE, second) > secondBefore)
    }

    @Test
    fun deletingATopicTombstonesItAndTouchesTheSurvivorsItRenumbers() = runBlocking {
        val phase = env.phases.add(env.curricula.create("Kotlin"), "Basics", "")
        val first = env.topics.add(phase, "One")
        val second = env.topics.add(phase, "Two")
        val secondBefore = stamp(SyncKind.TOPIC, second)

        env.topics.delete(first)

        assertTrue(deleted(SyncKind.TOPIC, first))
        assertTrue(stamp(SyncKind.TOPIC, second) > secondBefore)
    }

    @Test
    fun deletingTheLastPhaseTouchesNobodyElse() = runBlocking {
        val curriculum = env.curricula.create("Kotlin")
        val first = env.phases.add(curriculum, "One", "")
        val second = env.phases.add(curriculum, "Two", "")
        val firstBefore = stamp(SyncKind.PHASE, first)

        env.phases.delete(second)

        assertEquals(firstBefore, stamp(SyncKind.PHASE, first))
    }

    @Test
    fun movingOnlyStampsTheRowsWhoseNumberChanged() = runBlocking {
        val curriculum = env.curricula.create("Kotlin")
        val a = env.phases.add(curriculum, "A", "")
        val b = env.phases.add(curriculum, "B", "")
        val c = env.phases.add(curriculum, "C", "")
        val before = listOf(a, b, c).map { stamp(SyncKind.PHASE, it) }

        env.phases.move(curriculum, from = 0, to = 1)

        assertTrue(stamp(SyncKind.PHASE, a) > before[0])
        assertTrue(stamp(SyncKind.PHASE, b) > before[1])
        assertEquals(before[2], stamp(SyncKind.PHASE, c))
    }

    @Test
    fun movingTopicsOnlyStampsTheRowsWhoseNumberChanged() = runBlocking {
        val phase = env.phases.add(env.curricula.create("Kotlin"), "Basics", "")
        val a = env.topics.add(phase, "A")
        val b = env.topics.add(phase, "B")
        val c = env.topics.add(phase, "C")
        val before = listOf(a, b, c).map { stamp(SyncKind.TOPIC, it) }

        env.topics.move(phase, from = 2, to = 1)

        assertEquals(before[0], stamp(SyncKind.TOPIC, a))
        assertTrue(stamp(SyncKind.TOPIC, b) > before[1])
        assertTrue(stamp(SyncKind.TOPIC, c) > before[2])
    }

    @Test
    fun everyProgressChangeStampsItsOwnRecord() = runBlocking {
        val phase = env.phases.add(env.curricula.create("Kotlin"), "Basics", "")
        val topic = env.topics.add(phase, "Reading", total = 10, unit = "pages")

        env.progress.start(phase)
        val started = stamp(SyncKind.PHASE_STATUS, phase)
        env.progress.startTopic(topic)
        val topicStarted = stamp(SyncKind.TOPIC_PROGRESS, topic)
        env.progress.recordTopicProgress(topic, 3)
        val three = stamp(SyncKind.TOPIC_PROGRESS, topic)
        env.progress.recordTopicProgress(topic, 2)
        val two = stamp(SyncKind.TOPIC_PROGRESS, topic)
        env.progress.completeTopic(topic)
        val done = stamp(SyncKind.TOPIC_PROGRESS, topic)
        env.progress.complete(phase)

        assertTrue(topicStarted < three && three < two && two < done)
        assertTrue(stamp(SyncKind.PHASE_STATUS, phase) > started)
    }

    @Test
    fun editingARowThatDoesNotExistStampsNothing() = runBlocking {
        env.curricula.rename("ghost", "Nope")
        env.phases.update("ghost", "Nope", "")
        env.topics.update("ghost", "Nope", null, null)

        assertTrue(meta().isEmpty())
    }

    @Test
    fun aRejectedChangeLeavesNoStampBehind() = runBlocking {
        val curriculum = env.curricula.create("Kotlin")
        val first = env.phases.add(curriculum, "One", "")
        val second = env.phases.add(curriculum, "Two", "")
        env.progress.start(first)
        val before = meta()

        assertThrows(IllegalStateException::class.java) { runBlocking { env.progress.start(second) } }

        assertEquals(before, meta())
        assertFalse(meta().containsKey(RecordKey(SyncKind.PHASE_STATUS, second)))
    }

    @Test
    fun theStampsCarryTheDeviceOfTheTracker() = runBlocking {
        env.curricula.create("Kotlin")

        assertEquals("test", meta().values.single().stamp.deviceId)
    }
}
