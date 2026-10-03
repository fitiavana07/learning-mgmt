package dev.fitiavana.learning_mgmt.features.progress

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PhaseStatusDao {
    @Query(ROWS_BY_CURRICULUM)
    fun observeRowsByCurriculum(curriculumId: String): Flow<List<PhaseStatusRow>>

    @Query(ROWS_BY_CURRICULUM)
    suspend fun getRowsByCurriculum(curriculumId: String): List<PhaseStatusRow>

    @Query(
        """
        SELECT phase.*, phase_status.status AS status
        FROM phase LEFT JOIN phase_status ON phase_status.phaseId = phase.id
        ORDER BY phase.curriculumId, phase.number
        """,
    )
    fun observeAllRows(): Flow<List<PhaseStatusRow>>

    /** The status of a phase; null when it has no row yet (not started). */
    @Query("SELECT status FROM phase_status WHERE phaseId = :phaseId")
    suspend fun statusOf(phaseId: String): Status?

    @Upsert
    suspend fun upsert(status: PhaseStatus)

    @Query("SELECT COUNT(*) FROM phase_status")
    suspend fun count(): Int

    private companion object {
        const val ROWS_BY_CURRICULUM = """
            SELECT phase.*, phase_status.status AS status
            FROM phase LEFT JOIN phase_status ON phase_status.phaseId = phase.id
            WHERE phase.curriculumId = :curriculumId
            ORDER BY phase.number
        """
    }
}
