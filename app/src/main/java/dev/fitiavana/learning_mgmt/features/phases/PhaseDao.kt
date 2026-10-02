package dev.fitiavana.learning_mgmt.features.phases

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhaseDao {
    @Query("SELECT * FROM phase WHERE curriculumId = :curriculumId ORDER BY number")
    fun observeByCurriculum(curriculumId: String): Flow<List<Phase>>

    @Query("SELECT * FROM phase WHERE curriculumId = :curriculumId ORDER BY number")
    suspend fun getByCurriculum(curriculumId: String): List<Phase>

    @Query("SELECT * FROM phase WHERE id = :id")
    suspend fun get(id: String): Phase?

    @Insert
    suspend fun insert(phase: Phase)

    @Update
    suspend fun updateAll(phases: List<Phase>)

    @Query("UPDATE phase SET name = :name, description = :description WHERE id = :id")
    suspend fun updateDetails(id: String, name: String, description: String)

    @Query("DELETE FROM phase WHERE id = :id")
    suspend fun delete(id: String)
}
