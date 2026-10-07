package dev.fitiavana.learning_mgmt.features.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic

/**
 * Whole-table reads and writes for backup and restore. Rows are read in `rowid` (insertion)
 * order, which is the only order curricula have, so restoring them in list order round-trips.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM curriculum ORDER BY rowid")
    suspend fun curricula(): List<Curriculum>

    @Query("SELECT * FROM phase ORDER BY rowid")
    suspend fun phases(): List<Phase>

    @Query("SELECT * FROM phase_status ORDER BY rowid")
    suspend fun phaseStatuses(): List<PhaseStatus>

    @Query("SELECT * FROM topic ORDER BY rowid")
    suspend fun topics(): List<Topic>

    @Query("SELECT * FROM topic_progress ORDER BY rowid")
    suspend fun topicProgress(): List<TopicProgress>

    @Insert
    suspend fun insertCurricula(curricula: List<Curriculum>)

    @Insert
    suspend fun insertPhases(phases: List<Phase>)

    @Insert
    suspend fun insertPhaseStatuses(statuses: List<PhaseStatus>)

    @Insert
    suspend fun insertTopics(topics: List<Topic>)

    @Insert
    suspend fun insertTopicProgress(progress: List<TopicProgress>)

    /** Deleting the curricula cascades to phases, topics and their progress. */
    @Query("DELETE FROM curriculum")
    suspend fun deleteAll()
}
