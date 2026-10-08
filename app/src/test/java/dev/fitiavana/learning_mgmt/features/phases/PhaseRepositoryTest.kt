package dev.fitiavana.learning_mgmt.features.phases

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.db.testTracker
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
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
class PhaseRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var phases: PhaseRepository
    private lateinit var curriculumId: String

    @Before
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        val tracker = testTracker(db)
        curriculumId = CurriculumRepository(db, db.curriculumDao(), tracker, sequentialIds("c")).create("Spanish")
        phases = PhaseRepository(db, db.phaseDao(), tracker, sequentialIds("p"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun names() = phases.observe(curriculumId).first().map { it.number to it.name }

    @Test
    fun startsEmpty() = runBlocking {
        assertEquals(emptyList<Phase>(), phases.observe(curriculumId).first())
    }

    @Test
    fun addAppendsWithTheNextNumber() = runBlocking {
        val id = phases.add(curriculumId, "Basics", "## Intro")
        phases.add(curriculumId, "Verbs", "")

        assertEquals("p1", id)
        assertEquals(listOf(1 to "Basics", 2 to "Verbs"), names())
        assertEquals(Phase("p1", curriculumId, 1, "Basics", "## Intro"), phases.observe(curriculumId).first()[0])
    }

    @Test
    fun updateChangesNameAndDescriptionButNotNumber() = runBlocking {
        phases.add(curriculumId, "Basics", "")
        val id = phases.add(curriculumId, "Verbs", "")

        phases.update(id, "Past tenses", "new *text*")

        val updated = phases.observe(curriculumId).first()[1]
        assertEquals(Phase(id, curriculumId, 2, "Past tenses", "new *text*"), updated)
    }

    @Test
    fun deleteRenumbersTheRemainingPhases() = runBlocking {
        val first = phases.add(curriculumId, "A", "")
        phases.add(curriculumId, "B", "")
        phases.add(curriculumId, "C", "")

        phases.delete(first)

        assertEquals(listOf(1 to "B", 2 to "C"), names())
    }

    @Test
    fun moveReordersAndRenumbers() = runBlocking {
        phases.add(curriculumId, "A", "")
        phases.add(curriculumId, "B", "")
        phases.add(curriculumId, "C", "")

        phases.move(curriculumId, from = 0, to = 2)

        assertEquals(listOf(1 to "B", 2 to "C", 3 to "A"), names())
    }

    @Test
    fun phasesOfOtherCurriculaAreUntouched() = runBlocking {
        val other = CurriculumRepository(db, db.curriculumDao(), testTracker(db), sequentialIds("other")).create("Piano")
        phases.add(curriculumId, "A", "")
        phases.add(other, "X", "")
        phases.add(other, "Y", "")

        phases.delete(phases.observe(curriculumId).first().single().id)

        assertEquals(listOf(1 to "X", 2 to "Y"), phases.observe(other).first().map { it.number to it.name })
    }

    @Test
    fun blankNamesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { phases.add(curriculumId, "  ", "") }
        }
    }

    @Test
    fun deletingTheCurriculumDeletesItsPhases() = runBlocking {
        phases.add(curriculumId, "A", "")

        CurriculumRepository(db, db.curriculumDao(), testTracker(db), sequentialIds("c")).delete(curriculumId)

        assertEquals(emptyList<Phase>(), phases.observe(curriculumId).first())
    }
}
