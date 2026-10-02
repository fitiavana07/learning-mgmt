package dev.fitiavana.learning_mgmt.features.selection

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.db.sequentialIds
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CurriculumSelectionTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var curricula: CurriculumRepository
    private lateinit var selection: CurriculumSelection

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        curricula = CurriculumRepository(db.curriculumDao(), sequentialIds("c"))
        selection = CurriculumSelection(curricula, testSelectionStore(folder.root, scope))
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    private suspend fun selectedWhere(predicate: (Curriculum?) -> Boolean) =
        withTimeout(10_000) { selection.selected.first(predicate) }

    @Test
    fun nothingIsSelectedWithoutCurricula() = runBlocking {
        assertNull(selection.selected.first())
    }

    @Test
    fun theFirstCurriculumIsSelectedByDefault() = runBlocking {
        curricula.create("Spanish")
        curricula.create("Piano")

        assertEquals(Curriculum("c1", "Spanish"), selectedWhere { it != null })
    }

    @Test
    fun selectSwitchesTheSelectedCurriculum() = runBlocking {
        curricula.create("Spanish")
        val piano = curricula.create("Piano")

        selection.select(piano)

        assertEquals(Curriculum(piano, "Piano"), selectedWhere { it?.id == piano })
    }

    @Test
    fun fallsBackToTheFirstWhenTheSelectedCurriculumIsDeleted() = runBlocking {
        curricula.create("Spanish")
        val piano = curricula.create("Piano")
        selection.select(piano)
        selectedWhere { it?.id == piano }

        curricula.delete(piano)

        assertEquals("Spanish", selectedWhere { it?.id == "c1" }?.name)
    }

    @Test
    fun followsRenamesOfTheSelectedCurriculum() = runBlocking {
        val id = curricula.create("Spanish")
        selectedWhere { it != null }

        curricula.rename(id, "Spanish A2")

        assertEquals("Spanish A2", selectedWhere { it?.name == "Spanish A2" }?.name)
    }
}
