package dev.fitiavana.learning_mgmt.ui.managecurricula

import dev.fitiavana.learning_mgmt.db.DB_VERSION
import dev.fitiavana.learning_mgmt.db.TestEnvironment
import dev.fitiavana.learning_mgmt.features.backup.BackupCounts
import dev.fitiavana.learning_mgmt.features.backup.BackupJson
import dev.fitiavana.learning_mgmt.features.backup.BackupSummary
import dev.fitiavana.learning_mgmt.features.backup.RestoreResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ManageCurriculaViewModelTest {
    @get:Rule
    val env = TestEnvironment()

    private lateinit var viewModel: ManageCurriculaViewModel

    @Before
    fun setUp() {
        viewModel = env.track(ManageCurriculaViewModel(env.curricula, env.progress, env.backup))
    }

    private suspend fun rowsWhere(predicate: (List<CurriculumRow>) -> Boolean): List<CurriculumRow> =
        withTimeout(10_000) { viewModel.rows.first(predicate) }

    @Test
    fun isEmptyWithoutCurricula() = runBlocking {
        assertEquals(emptyList<CurriculumRow>(), rowsWhere { true })
    }

    @Test
    fun listsCurriculaWithTheirPhaseCount() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.curricula.create("Piano")
        env.phases.add(spanish, "Basics", "")
        env.phases.add(spanish, "Verbs", "")

        val rows = rowsWhere { it.size == 2 && it.first().phaseCount == 2 }

        assertEquals(listOf("Spanish" to 2, "Piano" to 0), rows.map { it.name to it.phaseCount })
    }

    @Test
    fun createAddsACurriculum() = runBlocking {
        viewModel.create("  Spanish ")

        assertEquals(listOf("Spanish"), rowsWhere { it.isNotEmpty() }.map { it.name })
    }

    @Test
    fun renameChangesTheName() = runBlocking {
        val id = env.curricula.create("Spanish")
        rowsWhere { it.isNotEmpty() }

        viewModel.rename(id, "Español")

        assertEquals(listOf("Español"), rowsWhere { it.singleOrNull()?.name == "Español" }.map { it.name })
    }

    @Test
    fun deleteRemovesTheCurriculumAndItsPhases() = runBlocking {
        val spanish = env.curricula.create("Spanish")
        env.curricula.create("Piano")
        env.phases.add(spanish, "Basics", "")
        rowsWhere { it.size == 2 }

        viewModel.delete(spanish)

        assertEquals(listOf("Piano"), rowsWhere { it.size == 1 }.map { it.name })
        assertEquals(emptyList<Any>(), env.phases.observe(spanish).first())
    }

    // Backup and restore

    private suspend fun message() = withTimeout(10_000) { viewModel.messages.first() }

    private suspend fun backupJson(): String {
        val spanish = env.curricula.create("Spanish")
        env.phases.add(spanish, "Basics", "")
        return env.backup.export()
    }

    /** Picks [json] as the restore file and waits until the confirmation is pending. */
    private suspend fun pick(json: String): PendingRestore {
        viewModel.restoreFrom { json }
        return withTimeout(10_000) { viewModel.pendingRestore.first { it != null }!! }
    }

    @Test
    fun backupHandsTheExportToTheWriterAndReportsIt() = runBlocking {
        env.curricula.create("Spanish")
        var written: String? = null

        viewModel.backup { written = it }

        assertEquals(BackupMessage.BackupSaved, message())
        val data = (BackupJson.decode(written!!) as BackupJson.DecodeResult.Success).data
        assertEquals(listOf("Spanish"), data.curricula.map { it.name })
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun aFailingWriterReportsAFailedBackup() = runBlocking {
        viewModel.backup { throw IOException("disk full") }

        assertEquals(BackupMessage.BackupFailed, message())
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun theViewModelIsBusyWhileTheBackupIsBeingWritten() = runBlocking {
        val release = CompletableDeferred<Unit>()

        viewModel.backup { release.await() }
        assertTrue(viewModel.busy.value)

        release.complete(Unit)
        assertEquals(BackupMessage.BackupSaved, message())
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun pickingAValidFileAsksForConfirmationWithoutChangingAnything() = runBlocking {
        val json = backupJson()
        env.curricula.create("Piano")

        val pending = pick(json)

        assertEquals(BackupSummary(curricula = 1, phases = 1, topics = 0, exportedAt = pending.summary.exportedAt), pending.summary)
        assertEquals(BackupCounts(curricula = 2, phases = 1, topics = 0), pending.current)
        assertEquals(2, env.backup.currentCounts().curricula)
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun confirmingReplacesTheDataAndReportsIt() = runBlocking {
        val json = backupJson()
        env.curricula.create("Piano")
        pick(json)

        viewModel.confirmRestore()

        assertEquals(BackupMessage.RestoreComplete, message())
        assertNull(viewModel.pendingRestore.value)
        assertEquals(listOf("Spanish"), env.curricula.observeAll().first().map { it.name })
    }

    @Test
    fun dismissingTheConfirmationChangesNothing() = runBlocking {
        val json = backupJson()
        env.curricula.create("Piano")
        pick(json)

        viewModel.dismissRestore()

        assertNull(viewModel.pendingRestore.value)
        assertEquals(2, env.backup.currentCounts().curricula)
    }

    @Test
    fun aFileThatIsNotABackupIsRejected() = runBlocking {
        viewModel.restoreFrom { "not json" }

        assertTrue((message() as BackupMessage.RestoreRejected).failure is RestoreResult.Error)
        assertNull(viewModel.pendingRestore.value)
    }

    @Test
    fun aBackupFromAnotherVersionIsRejected() = runBlocking {
        viewModel.restoreFrom { """{"schemaVersion": 1}""" }

        assertEquals(
            BackupMessage.RestoreRejected(RestoreResult.SchemaMismatch(backupVersion = 1, currentVersion = DB_VERSION)),
            message(),
        )
    }

    @Test
    fun anUnreadableFileIsReported() = runBlocking {
        viewModel.restoreFrom { throw IOException("gone") }

        assertEquals(BackupMessage.FileUnreadable, message())
        assertFalse(viewModel.busy.value)
    }
}
