package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
import dev.fitiavana.learning_mgmt.features.topics.TopicRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopicProgressRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var progress: ProgressRepository
    private lateinit var topics: TopicRepository
    private lateinit var phase: String
    private lateinit var otherPhase: String
    private lateinit var simple: String
    private lateinit var pages: String
    private lateinit var last: String

    @Before
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        val curriculumId = CurriculumRepository(db.curriculumDao(), sequentialIds("c")).create("Spanish")
        val phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        topics = TopicRepository(db, db.topicDao(), sequentialIds("t"))
        progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao(), db.topicProgressDao())
        phase = phases.add(curriculumId, "Basics", "")
        otherPhase = phases.add(curriculumId, "Verbs", "")
        simple = topics.add(phase, "Greetings")
        pages = topics.add(phase, "Chapter 1", total = 40, unit = "pages")
        last = topics.add(phase, "Numbers")
    }

    @After
    fun tearDown() = db.close()

    private suspend fun states() =
        progress.observeTopics(phase).first().map { Triple(it.topic.number, it.status, it.done) }

    private fun fails(block: suspend () -> Unit) {
        assertThrows(IllegalStateException::class.java) { runBlocking { block() } }
    }

    @Test
    fun topicsStartNotStartedWithNothingDone() = runBlocking {
        progress.start(phase)

        assertEquals(
            listOf(Triple(1, Status.NOT_STARTED, 0), Triple(2, Status.NOT_STARTED, 0), Triple(3, Status.NOT_STARTED, 0)),
            states(),
        )
    }

    @Test
    fun topicsAreStartedAndCompletedOneAtATimeInOrder() = runBlocking {
        progress.start(phase)

        fails { progress.startTopic(pages) }
        progress.startTopic(simple)
        fails { progress.startTopic(pages) }
        progress.completeTopic(simple)
        progress.startTopic(pages)

        assertEquals(
            listOf(Triple(1, Status.COMPLETED, 0), Triple(2, Status.IN_PROGRESS, 0), Triple(3, Status.NOT_STARTED, 0)),
            states(),
        )
    }

    @Test
    fun aTopicCannotBeCompletedUnlessItIsInProgress() = runBlocking {
        progress.start(phase)

        fails { progress.completeTopic(simple) }
    }

    @Test
    fun topicsCannotChangeUnlessTheirPhaseIsInProgress() = runBlocking {
        fails { progress.startTopic(simple) }

        progress.start(phase)
        progress.startTopic(simple)
        progress.completeTopic(simple)
        progress.startTopic(pages)
        progress.recordTopicProgress(pages, 40)
        progress.startTopic(last)
        progress.completeTopic(last)
        progress.complete(phase)

        fails { progress.recordTopicProgress(pages, 3) }
    }

    @Test
    fun recordingProgressKeepsTheTopicInProgressUntilTheTotalIsReached() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)
        progress.completeTopic(simple)
        progress.startTopic(pages)

        progress.recordTopicProgress(pages, 12)
        assertEquals(Triple(2, Status.IN_PROGRESS, 12), states()[1])

        progress.recordTopicProgress(pages, 40)
        assertEquals(Triple(2, Status.COMPLETED, 40), states()[1])
    }

    @Test
    fun recordedProgressIsClampedToTheTotal() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)
        progress.completeTopic(simple)
        progress.startTopic(pages)

        progress.recordTopicProgress(pages, 999)

        assertEquals(Triple(2, Status.COMPLETED, 40), states()[1])
    }

    @Test
    fun completingAQuantifiedTopicFillsItsTotal() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)
        progress.completeTopic(simple)
        progress.startTopic(pages)
        progress.recordTopicProgress(pages, 5)

        progress.completeTopic(pages)

        assertEquals(Triple(2, Status.COMPLETED, 40), states()[1])
    }

    @Test
    fun progressCannotBeRecordedOnASimpleOrNotCurrentTopic() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)

        fails { progress.recordTopicProgress(simple, 1) }
        fails { progress.recordTopicProgress(pages, 1) }
    }

    @Test
    fun loweringATotalBelowTheRecordedProgressClampsAndCompletesTheTopic() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)
        progress.completeTopic(simple)
        progress.startTopic(pages)
        progress.recordTopicProgress(pages, 30)

        topics.update(pages, "Chapter 1", total = 20, unit = "pages")

        assertEquals(Triple(2, Status.COMPLETED, 20), states()[1])
    }

    @Test
    fun aPhaseWithUncompletedTopicsCannotBeCompleted() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)

        fails { progress.complete(phase) }
    }

    @Test
    fun aPhaseWithAllTopicsCompletedCanBeCompleted() = runBlocking {
        progress.start(phase)
        progress.startTopic(simple)
        progress.completeTopic(simple)
        progress.startTopic(pages)
        progress.completeTopic(pages)
        progress.startTopic(last)
        progress.completeTopic(last)

        progress.complete(phase)

        assertEquals(Status.COMPLETED, progress.observe(db.phaseDao().get(phase)!!.curriculumId).first()[0].status)
    }

    @Test
    fun aPhaseWithoutTopicsCanStillBeCompleted() = runBlocking {
        progress.start(phase)
        topics.delete(simple)
        topics.delete(pages)
        topics.delete(last)

        progress.complete(phase)
    }

    @Test
    fun topicsOfOtherPhasesAreNotShown() = runBlocking {
        topics.add(otherPhase, "Other")

        assertEquals(3, progress.observeTopics(phase).first().size)
        assertEquals(1, progress.observeTopics(otherPhase).first().size)
    }
}
