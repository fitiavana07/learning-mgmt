package dev.fitiavana.learning_mgmt.features.backup

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.features.selection.LaunchSelection
import dev.fitiavana.learning_mgmt.features.selection.SelectedCurriculumStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant

sealed interface RestoreResult {
    data object Success : RestoreResult

    sealed interface Failure : RestoreResult

    /** The backup was made by a different database version; backups are not migrated. */
    data class SchemaMismatch(val backupVersion: Int, val currentVersion: Int) : Failure

    data class Error(val message: String) : Failure
}

/** What a backup file holds, shown before the user confirms a restore. */
data class BackupSummary(val curricula: Int, val phases: Int, val topics: Int, val exportedAt: String)

/** What the app holds now, i.e. what a restore would erase. */
data class BackupCounts(val curricula: Int, val phases: Int, val topics: Int)

/** The result of reading a backup file without touching the database. */
sealed interface Inspection {
    data class Valid(val data: BackupData) : Inspection {
        val summary = BackupSummary(
            curricula = data.curricula.size,
            phases = data.phases.size,
            topics = data.topics.size,
            exportedAt = data.exportedAt,
        )
    }

    data class Rejected(val failure: RestoreResult.Failure) : Inspection
}

/**
 * Full backup and restore of the database plus the selected curriculum. Restore replaces
 * everything: the file is fully read and validated first, then applied in one transaction, so a
 * bad file or a failure halfway leaves the current data untouched.
 */
class BackupRepository(
    private val db: RoomDatabase,
    private val dao: BackupDao,
    private val selectionStore: SelectedCurriculumStore,
    private val schemaVersion: Int,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun export(): String {
        val data = db.withTransaction {
            BackupData(
                schemaVersion = schemaVersion,
                exportedAt = Instant.now(clock).toString(),
                selectedCurriculumId = selectionStore.selectedId.first(),
                curricula = dao.curricula(),
                phases = dao.phases(),
                phaseStatuses = dao.phaseStatuses(),
                topics = dao.topics(),
                topicProgress = dao.topicProgress(),
            )
        }
        return BackupJson.encode(data)
    }

    suspend fun currentCounts() = db.withTransaction {
        BackupCounts(dao.curricula().size, dao.phases().size, dao.topics().size)
    }

    fun inspect(json: String): Inspection {
        BackupJson.schemaVersion(json)?.takeIf { it != schemaVersion }?.let {
            return Inspection.Rejected(RestoreResult.SchemaMismatch(backupVersion = it, currentVersion = schemaVersion))
        }
        val data = when (val decoded = BackupJson.decode(json)) {
            is BackupJson.DecodeResult.Invalid -> return Inspection.Rejected(RestoreResult.Error(decoded.message))
            is BackupJson.DecodeResult.Success -> decoded.data
        }
        BackupRules.violation(data)?.let { return Inspection.Rejected(RestoreResult.Error(it)) }
        return Inspection.Valid(data)
    }

    suspend fun restore(json: String): RestoreResult {
        val data = when (val inspection = inspect(json)) {
            is Inspection.Rejected -> return inspection.failure
            is Inspection.Valid -> inspection.data
        }
        return try {
            db.withTransaction {
                dao.deleteAll()
                dao.insertCurricula(data.curricula)
                dao.insertPhases(data.phases)
                dao.insertTopics(data.topics)
                dao.insertPhaseStatuses(data.phaseStatuses)
                dao.insertTopicProgress(data.topicProgress)
            }
            selectionStore.select(LaunchSelection.resolve(data.selectedCurriculumId, data.curricula.map { it.id }))
            RestoreResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RestoreResult.Error("The backup could not be restored: ${e.message ?: "unknown error"}")
        }
    }
}
