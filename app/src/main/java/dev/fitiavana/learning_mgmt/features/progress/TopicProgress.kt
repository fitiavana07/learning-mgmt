package dev.fitiavana.learning_mgmt.features.progress

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.fitiavana.learning_mgmt.features.topics.Topic

/** Progress of one topic. A topic without a row is NOT_STARTED with nothing done. */
@Entity(
    tableName = "topic_progress",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TopicProgress(
    @PrimaryKey val topicId: String,
    val status: Status,
    val done: Int,
)

/** A topic joined with its optional progress row. */
data class TopicProgressRow(
    @Embedded val topic: Topic,
    val status: Status?,
    val done: Int?,
) {
    /** Progress recorded against an older total is clamped to the current one. */
    fun toTopicWithProgress(): TopicWithProgress {
        val stored = TopicWithProgress(topic, status ?: Status.NOT_STARTED, done ?: 0)
        return TopicRules.record(stored, stored.done)
    }
}
