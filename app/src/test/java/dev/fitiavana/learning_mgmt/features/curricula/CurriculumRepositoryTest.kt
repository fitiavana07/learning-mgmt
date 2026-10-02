package dev.fitiavana.learning_mgmt.features.curricula

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
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
class CurriculumRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: CurriculumRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun startsEmpty() = runBlocking {
        assertEquals(emptyList<Curriculum>(), repository.observeAll().first())
    }

    @Test
    fun createReturnsTheGeneratedIdAndStoresTheCurriculum() = runBlocking {
        val id = repository.create("Spanish A2")

        assertEquals("c1", id)
        assertEquals(listOf(Curriculum("c1", "Spanish A2")), repository.observeAll().first())
    }

    @Test
    fun curriculaAreListedInCreationOrder() = runBlocking {
        repository.create("Zulu")
        repository.create("Arabic")

        assertEquals(listOf("Zulu", "Arabic"), repository.observeAll().first().map { it.name })
    }

    @Test
    fun renameChangesTheNameAndKeepsTheId() = runBlocking {
        val id = repository.create("Spanish")

        repository.rename(id, "Spanish A2")

        assertEquals(listOf(Curriculum(id, "Spanish A2")), repository.observeAll().first())
    }

    @Test
    fun deleteRemovesTheCurriculum() = runBlocking {
        val keep = repository.create("Keep")
        val drop = repository.create("Drop")

        repository.delete(drop)

        assertEquals(listOf(keep), repository.observeAll().first().map { it.id })
    }

    @Test
    fun blankNamesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.create("   ") } }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.rename(repository.create("Ok"), "") }
        }
    }

    @Test
    fun namesAreTrimmed() = runBlocking {
        repository.create("  Piano  ")

        assertEquals("Piano", repository.observeAll().first().single().name)
    }
}
