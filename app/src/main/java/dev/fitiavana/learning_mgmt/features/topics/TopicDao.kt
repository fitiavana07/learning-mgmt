package dev.fitiavana.learning_mgmt.features.topics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topic WHERE phaseId = :phaseId ORDER BY number")
    fun observeByPhase(phaseId: String): Flow<List<Topic>>

    @Query("SELECT * FROM topic WHERE phaseId = :phaseId ORDER BY number")
    suspend fun getByPhase(phaseId: String): List<Topic>

    @Query("SELECT * FROM topic WHERE id = :id")
    suspend fun get(id: String): Topic?

    @Insert
    suspend fun insert(topic: Topic)

    @Update
    suspend fun updateAll(topics: List<Topic>)

    @Query("UPDATE topic SET name = :name, total = :total, unit = :unit WHERE id = :id")
    suspend fun updateDetails(id: String, name: String, total: Int?, unit: String?)

    @Query("DELETE FROM topic WHERE id = :id")
    suspend fun delete(id: String)
}
