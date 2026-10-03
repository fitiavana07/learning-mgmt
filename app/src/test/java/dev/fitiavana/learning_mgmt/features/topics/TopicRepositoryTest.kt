package dev.fitiavana.learning_mgmt.features.topics

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.phases.PhaseRepository
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
class TopicRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var phases: PhaseRepository
    private lateinit var topics: TopicRepository
    private lateinit var curriculumId: String
    private lateinit var phaseId: String

    @Before
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        curriculumId = CurriculumRepository(db.curriculumDao(), sequentialIds("c")).create("Spanish")
        phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        phaseId = phases.add(curriculumId, "Basics", "")
        topics = TopicRepository(db, db.topicDao(), sequentialIds("t"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun names() = topics.observe(phaseId).first().map { it.number to it.name }

    @Test
    fun startsEmpty() = runBlocking {
        assertEquals(emptyList<Topic>(), topics.observe(phaseId).first())
    }

    @Test
    fun addAppendsASimpleTopicWithTheNextNumber() = runBlocking {
        val id = topics.add(phaseId, "  Greetings ")
        topics.add(phaseId, "Numbers")

        assertEquals("t1", id)
        assertEquals(listOf(1 to "Greetings", 2 to "Numbers"), names())
        assertEquals(Topic("t1", phaseId, 1, "Greetings", total = null, unit = null), topics.observe(phaseId).first()[0])
    }

    @Test
    fun addStoresATotalAndATrimmedUnitForAQuantifiedTopic() = runBlocking {
        topics.add(phaseId, "Chapter 1", total = 40, unit = " pages ")

        assertEquals(Topic("t1", phaseId, 1, "Chapter 1", total = 40, unit = "pages"), topics.observe(phaseId).first()[0])
    }

    @Test
    fun aBlankUnitIsStoredAsNoUnit() = runBlocking {
        topics.add(phaseId, "Chapter 1", total = 40, unit = "  ")

        assertEquals(null, topics.observe(phaseId).first()[0].unit)
    }

    @Test
    fun aSimpleTopicNeverKeepsAUnit() = runBlocking {
        topics.add(phaseId, "Greetings", total = null, unit = "pages")

        assertEquals(null, topics.observe(phaseId).first()[0].unit)
    }

    @Test
    fun rejectsABlankNameAndANonPositiveTotal() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { topics.add(phaseId, "  ") } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { topics.add(phaseId, "A", total = 0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { topics.add(phaseId, "A", total = -2) } }
    }

    @Test
    fun updateChangesNameTotalAndUnitButNotNumber() = runBlocking {
        topics.add(phaseId, "Greetings")
        val id = topics.add(phaseId, "Numbers")

        topics.update(id, "Big numbers", total = 12, unit = "exercises")

        assertEquals(
            Topic(id, phaseId, 2, "Big numbers", total = 12, unit = "exercises"),
            topics.observe(phaseId).first()[1],
        )
    }

    @Test
    fun updateCanTurnAQuantifiedTopicIntoASimpleOne() = runBlocking {
        val id = topics.add(phaseId, "Chapter 1", total = 40, unit = "pages")

        topics.update(id, "Chapter 1", total = null, unit = "pages")

        assertEquals(Topic(id, phaseId, 1, "Chapter 1", total = null, unit = null), topics.observe(phaseId).first()[0])
    }

    @Test
    fun deleteRenumbersTheRemainingTopics() = runBlocking {
        val first = topics.add(phaseId, "A")
        topics.add(phaseId, "B")
        topics.add(phaseId, "C")

        topics.delete(first)

        assertEquals(listOf(1 to "B", 2 to "C"), names())
    }

    @Test
    fun moveReordersAndRenumbers() = runBlocking {
        topics.add(phaseId, "A")
        topics.add(phaseId, "B")
        topics.add(phaseId, "C")

        topics.move(phaseId, from = 2, to = 0)

        assertEquals(listOf(1 to "C", 2 to "A", 3 to "B"), names())
    }

    @Test
    fun topicsOfAPhaseAreIndependentOfOtherPhases() = runBlocking {
        val other = phases.add(curriculumId, "Verbs", "")
        topics.add(phaseId, "A")
        topics.add(other, "X")

        assertEquals(listOf(1 to "A"), names())
        assertEquals(listOf(1 to "X"), topics.observe(other).first().map { it.number to it.name })
    }

    @Test
    fun deletingAPhaseDeletesItsTopics() = runBlocking {
        topics.add(phaseId, "A")

        phases.delete(phaseId)

        assertEquals(emptyList<Topic>(), db.topicDao().getByPhase(phaseId))
    }
}
