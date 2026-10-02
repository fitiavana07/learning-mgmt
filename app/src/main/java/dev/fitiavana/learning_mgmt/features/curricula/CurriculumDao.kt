package dev.fitiavana.learning_mgmt.features.curricula

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM curriculum ORDER BY rowid")
    fun observeAll(): Flow<List<Curriculum>>

    @Insert
    suspend fun insert(curriculum: Curriculum)

    @Query("UPDATE curriculum SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM curriculum WHERE id = :id")
    suspend fun delete(id: String)
}
