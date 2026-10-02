package dev.fitiavana.learning_mgmt.features.progress

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
class ProgressRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var phases: PhaseRepository
    private lateinit var progress: ProgressRepository
    private lateinit var curriculumId: String
    private lateinit var p1: String
    private lateinit var p2: String
    private lateinit var p3: String

    @Before
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        curriculumId = CurriculumRepository(db.curriculumDao(), sequentialIds("c")).create("Spanish")
        phases = PhaseRepository(db, db.phaseDao(), sequentialIds("p"))
        progress = ProgressRepository(db, db.phaseDao(), db.phaseStatusDao())
        p1 = phases.add(curriculumId, "A", "")
        p2 = phases.add(curriculumId, "B", "")
        p3 = phases.add(curriculumId, "C", "")
    }

    @After
    fun tearDown() = db.close()

    private suspend fun statuses() =
        progress.observe(curriculumId).first().map { it.phase.number to it.status }

    private suspend fun statusRowCount(): Int = db.phaseStatusDao().count()

    @Test
    fun everyPhaseStartsNotStartedWithoutAnyStatusRow() = runBlocking {
        assertEquals(
            listOf(1 to Status.NOT_STARTED, 2 to Status.NOT_STARTED, 3 to Status.NOT_STARTED),
            statuses(),
        )
        assertEquals(0, statusRowCount())
    }

    @Test
    fun observeAllGroupsPhasesByCurriculumInOrder() = runBlocking {
        val piano = CurriculumRepository(db.curriculumDao(), sequentialIds("o")).create("Piano")
        PhaseRepository(db, db.phaseDao(), sequentialIds("q")).add(piano, "Scales", "")
        progress.start(p1)

        val all = progress.observeAll().first()

        assertEquals(setOf(curriculumId, piano), all.keys)
        assertEquals(
            listOf(1 to Status.IN_PROGRESS, 2 to Status.NOT_STARTED, 3 to Status.NOT_STARTED),
            all.getValue(curriculumId).map { it.phase.number to it.status },
        )
        assertEquals(listOf(Status.NOT_STARTED), all.getValue(piano).map { it.status })
    }

    @Test
    fun startMarksTheFirstPhaseInProgress() = runBlocking {
        progress.start(p1)

        assertEquals(
            listOf(1 to Status.IN_PROGRESS, 2 to Status.NOT_STARTED, 3 to Status.NOT_STARTED),
            statuses(),
        )
    }

    @Test
    fun cannotStartAPhaseOtherThanTheNextOne() = runBlocking {
        assertThrows(IllegalStateException::class.java) { runBlocking { progress.start(p2) } }
        assertEquals(0, statusRowCount())
    }

    @Test
    fun cannotStartWhileAnotherPhaseIsInProgress() = runBlocking {
        progress.start(p1)

        assertThrows(IllegalStateException::class.java) { runBlocking { progress.start(p2) } }
        Unit
    }

    @Test
    fun completeThenStartTheNextPhase() = runBlocking {
        progress.start(p1)
        progress.complete(p1)
        progress.start(p2)

        assertEquals(
            listOf(1 to Status.COMPLETED, 2 to Status.IN_PROGRESS, 3 to Status.NOT_STARTED),
            statuses(),
        )
    }

    @Test
    fun cannotCompleteAPhaseThatIsNotInProgress() = runBlocking {
        assertThrows(IllegalStateException::class.java) { runBlocking { progress.complete(p1) } }
        progress.start(p1)
        assertThrows(IllegalStateException::class.java) { runBlocking { progress.complete(p2) } }
        Unit
    }

    @Test
    fun statusesStayWithTheirPhaseWhenReordered() = runBlocking {
        progress.start(p1)
        progress.complete(p1)
        progress.start(p2)

        phases.move(curriculumId, from = 1, to = 0)

        assertEquals(
            listOf(1 to Status.IN_PROGRESS, 2 to Status.COMPLETED, 3 to Status.NOT_STARTED),
            statuses(),
        )
        assertEquals(2, statusRowCount())
    }

    @Test
    fun structureEditsLeaveProgressRowsAlone() = runBlocking {
        progress.start(p1)

        phases.update(p1, "Renamed", "text")
        phases.move(curriculumId, from = 0, to = 2)

        assertEquals(1, statusRowCount())
    }

    @Test
    fun deletingAPhaseDeletesItsStatusRow() = runBlocking {
        progress.start(p1)

        phases.delete(p1)

        assertEquals(0, statusRowCount())
        assertEquals(listOf(1 to Status.NOT_STARTED, 2 to Status.NOT_STARTED), statuses())
    }

    @Test
    fun deletingTheCurriculumDeletesItsStatusRows() = runBlocking {
        progress.start(p1)

        CurriculumRepository(db.curriculumDao(), sequentialIds("c")).delete(curriculumId)

        assertEquals(0, statusRowCount())
    }
}
