package dev.fitiavana.learning_mgmt.features.progress

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicProgressDao {
    @Query(ROWS_BY_PHASE)
    fun observeRowsByPhase(phaseId: String): Flow<List<TopicProgressRow>>

    @Query(ROWS_BY_PHASE)
    suspend fun getRowsByPhase(phaseId: String): List<TopicProgressRow>

    @Query("SELECT phaseId FROM topic WHERE id = :topicId")
    suspend fun phaseIdOf(topicId: String): String?

    @Upsert
    suspend fun upsert(progress: TopicProgress)

    private companion object {
        const val ROWS_BY_PHASE = """
            SELECT topic.*, topic_progress.status AS status, topic_progress.done AS done
            FROM topic LEFT JOIN topic_progress ON topic_progress.topicId = topic.id
            WHERE topic.phaseId = :phaseId
            ORDER BY topic.number
        """
    }
}
