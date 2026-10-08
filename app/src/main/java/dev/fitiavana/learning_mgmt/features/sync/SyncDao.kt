package dev.fitiavana.learning_mgmt.features.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum
import dev.fitiavana.learning_mgmt.features.phases.Phase
import dev.fitiavana.learning_mgmt.features.progress.PhaseStatus
import dev.fitiavana.learning_mgmt.features.progress.TopicProgress
import dev.fitiavana.learning_mgmt.features.topics.Topic

/**
 * Writes the rows a sync changes, straight into the tables: the repositories' own rules (start
 * in order...) are for local actions, a merge applies what another device already did. An upsert
 * updates a row in place, so it never triggers the foreign key cascades a replace would.
 */
@Dao
interface SyncDao {
    @Upsert
    suspend fun upsertCurricula(rows: List<Curriculum>)

    @Upsert
    suspend fun upsertPhases(rows: List<Phase>)

    @Upsert
    suspend fun upsertTopics(rows: List<Topic>)

    @Upsert
    suspend fun upsertPhaseStatuses(rows: List<PhaseStatus>)

    @Upsert
    suspend fun upsertTopicProgress(rows: List<TopicProgress>)

    @Query("DELETE FROM curriculum WHERE id IN (:ids)")
    suspend fun deleteCurricula(ids: List<String>)

    @Query("DELETE FROM phase WHERE id IN (:ids)")
    suspend fun deletePhases(ids: List<String>)

    @Query("DELETE FROM topic WHERE id IN (:ids)")
    suspend fun deleteTopics(ids: List<String>)

    @Query("DELETE FROM phase_status WHERE phaseId IN (:phaseIds)")
    suspend fun deletePhaseStatuses(phaseIds: List<String>)

    @Query("DELETE FROM topic_progress WHERE topicId IN (:topicIds)")
    suspend fun deleteTopicProgress(topicIds: List<String>)
}
