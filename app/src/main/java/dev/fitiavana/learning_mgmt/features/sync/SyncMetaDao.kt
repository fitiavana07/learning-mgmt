package dev.fitiavana.learning_mgmt.features.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * Version of one synced row, kept beside the data so the feature tables stay as they are. A
 * deleted row is gone from its own table and only lives on here as a tombstone.
 */
@Entity(tableName = "sync_meta", primaryKeys = ["kind", "id"])
data class SyncMetaRow(
    val kind: SyncKind,
    val id: String,
    val stampTime: Long,
    val stampDevice: String,
    val deleted: Boolean,
) {
    fun key() = RecordKey(kind, id)

    fun meta() = Meta(Stamp(stampTime, stampDevice), deleted)

    companion object {
        fun of(key: RecordKey, meta: Meta) =
            SyncMetaRow(key.kind, key.id, meta.stamp.time, meta.stamp.deviceId, meta.deleted)
    }
}

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta")
    suspend fun all(): List<SyncMetaRow>

    @Upsert
    suspend fun upsert(rows: List<SyncMetaRow>)

    @Query("DELETE FROM sync_meta")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(stampTime), 0) FROM sync_meta")
    suspend fun highestTime(): Long

    // What a deletion takes with it through the foreign key cascades, read before the delete.

    @Query("SELECT id FROM phase WHERE curriculumId = :curriculumId")
    suspend fun phaseIdsOfCurriculum(curriculumId: String): List<String>

    @Query("SELECT topic.id FROM topic JOIN phase ON phase.id = topic.phaseId WHERE phase.curriculumId = :curriculumId")
    suspend fun topicIdsOfCurriculum(curriculumId: String): List<String>

    @Query("SELECT id FROM topic WHERE phaseId = :phaseId")
    suspend fun topicIdsOfPhase(phaseId: String): List<String>

    @Query("SELECT phaseId FROM phase_status WHERE phaseId IN (:phaseIds)")
    suspend fun phaseStatusIds(phaseIds: List<String>): List<String>

    @Query("SELECT topicId FROM topic_progress WHERE topicId IN (:topicIds)")
    suspend fun topicProgressIds(topicIds: List<String>): List<String>
}
