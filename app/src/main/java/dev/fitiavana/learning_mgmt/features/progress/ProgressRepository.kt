package dev.fitiavana.learning_mgmt.features.progress

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.features.phases.PhaseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Progress only: reads phases joined with their status, and starts/completes them. */
class ProgressRepository(
    private val db: RoomDatabase,
    private val phaseDao: PhaseDao,
    private val statusDao: PhaseStatusDao,
) {
    fun observe(curriculumId: String): Flow<List<PhaseWithStatus>> =
        statusDao.observeRowsByCurriculum(curriculumId).map { rows -> rows.map { it.toPhaseWithStatus() } }

    suspend fun start(phaseId: String) =
        transition(phaseId, Status.IN_PROGRESS, PhaseRules::canStart, "start")

    suspend fun complete(phaseId: String) =
        transition(phaseId, Status.COMPLETED, PhaseRules::canComplete, "complete")

    private suspend fun transition(
        phaseId: String,
        to: Status,
        allowed: (List<PhaseWithStatus>, PhaseWithStatus) -> Boolean,
        verb: String,
    ) {
        db.withTransaction {
            val curriculumId = checkNotNull(phaseDao.get(phaseId)) { "Unknown phase $phaseId" }.curriculumId
            val phases = statusDao.getRowsByCurriculum(curriculumId).map { it.toPhaseWithStatus() }
            val target = phases.first { it.phase.id == phaseId }
            check(allowed(phases, target)) { "Phase ${target.phase.number} cannot $verb now" }
            statusDao.upsert(PhaseStatus(phaseId, to))
        }
    }
}
