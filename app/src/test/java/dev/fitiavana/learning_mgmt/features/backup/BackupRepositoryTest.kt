package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.db.AppDatabase
import dev.fitiavana.learning_mgmt.db.inMemoryDatabase
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.Status
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.selection.SelectedCurriculumStore
import dev.fitiavana.learning_mgmt.features.selection.testSelectionStore
import dev.fitiavana.learning_mgmt.features.topics.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var sourceDb: AppDatabase
    private lateinit var sourceStore: SelectedCurriculumStore
    private lateinit var source: BackupRepository
    private lateinit var targetDb: AppDatabase
    private lateinit var targetStore: SelectedCurriculumStore
    private lateinit var target: BackupRepository

    private val curricula = listOf(Curriculum("c2", "Spanish"), Curriculum("c1", "Math"))
    private val phases = listOf(
        Phase("p1", "c2", 1, "Basics", "# Intro"),
        Phase("p2", "c2", 2, "Verbs", ""),
        Phase("p3", "c1", 1, "Algebra", ""),
    )
    private val phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED), PhaseStatus("p2", Status.IN_PROGRESS))
    private val topics = listOf(
        Topic("t1", "p1", 1, "Greetings", null, null),
        Topic("t2", "p2", 1, "Reading", 40, "pages"),
    )
    private val topicProgress = listOf(
        TopicProgress("t1", Status.COMPLETED, 0),
        TopicProgress("t2", Status.IN_PROGRESS, 12),
    )

    private fun repository(db: AppDatabase, store: SelectedCurriculumStore, version: Int = VERSION) =
        BackupRepository(db, db.backupDao(), store, version, clock)

    @Before
    fun setUp() = runBlocking {
        sourceDb = inMemoryDatabase()
        sourceStore = testSelectionStore(Files.createTempDirectory("source").toFile(), scope)
        source = repository(sourceDb, sourceStore)
        targetDb = inMemoryDatabase()
        targetStore = testSelectionStore(Files.createTempDirectory("target").toFile(), scope)
        target = repository(targetDb, targetStore)

        with(sourceDb.backupDao()) {
            insertCurricula(curricula)
            insertPhases(phases)
            insertPhaseStatuses(phaseStatuses)
            insertTopics(topics)
            insertTopicProgress(topicProgress)
        }
        sourceStore.select("c1")
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
        scope.cancel()
    }

    private suspend fun contents(db: AppDatabase) = with(db.backupDao()) {
        listOf(curricula(), phases(), phaseStatuses(), topics(), topicProgress())
    }

    private suspend fun restoreFromSource() = target.restore(source.export())

    @Test
    fun exportWritesHeaderAndSelection() = runBlocking {
        val data = (BackupJson.decode(source.export()) as BackupJson.DecodeResult.Success).data

        assertEquals(VERSION, data.schemaVersion)
        assertEquals("2026-10-07T10:00:00Z", data.exportedAt)
        assertEquals("c1", data.selectedCurriculumId)
    }

    @Test
    fun restoreReproducesEverythingIncludingOrder() = runBlocking {
        assertEquals(RestoreResult.Success, restoreFromSource())

        assertEquals(contents(sourceDb), contents(targetDb))
        assertEquals(listOf("c2", "c1"), targetDb.backupDao().curricula().map { it.id })
        assertEquals("c1", targetStore.selectedId.first())
    }

    @Test
    fun restoreReplacesExistingData() = runBlocking {
        with(targetDb.backupDao()) {
            insertCurricula(listOf(Curriculum("old", "Old")))
            insertPhases(listOf(Phase("oldp", "old", 1, "Old phase", "")))
        }

        assertEquals(RestoreResult.Success, restoreFromSource())

        assertEquals(contents(sourceDb), contents(targetDb))
    }

    @Test
    fun restoredSelectionFallsBackToTheFirstCurriculumWhenItsIdIsMissing() = runBlocking {
        sourceStore.select("gone")

        assertEquals(RestoreResult.Success, restoreFromSource())

        assertEquals("c2", targetStore.selectedId.first())
    }

    @Test
    fun restoringAnEmptyBackupEmptiesTheApp() = runBlocking {
        val emptyDb = inMemoryDatabase()
        val emptyStore = testSelectionStore(Files.createTempDirectory("empty").toFile(), scope)
        val json = repository(emptyDb, emptyStore).export()
        targetStore.select("c1")
        with(targetDb.backupDao()) { insertCurricula(curricula) }

        assertEquals(RestoreResult.Success, target.restore(json))

        assertEquals(emptyList<Curriculum>(), targetDb.backupDao().curricula())
        assertEquals(null, targetStore.selectedId.first())
        emptyDb.close()
    }

    @Test
    fun aDifferentSchemaVersionIsRejectedAndNothingChanges() = runBlocking {
        val older = repository(targetDb, targetStore, version = VERSION - 1).export()
        targetDb.backupDao().insertCurricula(listOf(Curriculum("keep", "Keep")))

        val result = target.restore(older)

        assertEquals(RestoreResult.SchemaMismatch(backupVersion = VERSION - 1, currentVersion = VERSION), result)
        assertEquals(listOf(Curriculum("keep", "Keep")), targetDb.backupDao().curricula())
    }

    @Test
    fun aFileFromAnotherVersionIsAMismatchEvenIfItsShapeIsUnknown() = runBlocking {
        val result = target.restore("""{"schemaVersion": 9, "somethingNew": 1}""")

        assertEquals(RestoreResult.SchemaMismatch(backupVersion = 9, currentVersion = VERSION), result)
    }

    @Test
    fun aMalformedFileIsRejectedAndNothingChanges() = runBlocking {
        targetDb.backupDao().insertCurricula(listOf(Curriculum("keep", "Keep")))

        val result = target.restore("not json")

        assertTrue(result is RestoreResult.Error)
        assertEquals(listOf(Curriculum("keep", "Keep")), targetDb.backupDao().curricula())
    }

    @Test
    fun inconsistentDataIsRejectedAndNothingChanges() = runBlocking {
        val broken = BackupJson.encode(
            (BackupJson.decode(source.export()) as BackupJson.DecodeResult.Success).data
                .copy(phaseStatuses = listOf(PhaseStatus("p1", Status.COMPLETED), PhaseStatus("nope", Status.COMPLETED))),
        )
        targetDb.backupDao().insertCurricula(listOf(Curriculum("keep", "Keep")))

        val result = target.restore(broken)

        assertTrue(result is RestoreResult.Error)
        assertEquals(listOf(Curriculum("keep", "Keep")), targetDb.backupDao().curricula())
    }

    @Test
    fun inspectSummarizesAValidFile() = runBlocking {
        val inspection = target.inspect(source.export()) as Inspection.Valid

        assertEquals(BackupSummary(curricula = 2, phases = 3, topics = 2, exportedAt = "2026-10-07T10:00:00Z"), inspection.summary)
    }

    @Test
    fun inspectReportsWhyAFileIsRejected() {
        assertTrue((target.inspect("not json") as Inspection.Rejected).failure is RestoreResult.Error)
        assertEquals(
            RestoreResult.SchemaMismatch(backupVersion = 1, currentVersion = VERSION),
            (target.inspect("""{"schemaVersion": 1}""") as Inspection.Rejected).failure,
        )
    }

    @Test
    fun currentCountsDescribeWhatARestoreWouldErase() = runBlocking {
        assertEquals(BackupCounts(curricula = 2, phases = 3, topics = 2), source.currentCounts())
    }

    private companion object {
        const val VERSION = 2
    }
}
