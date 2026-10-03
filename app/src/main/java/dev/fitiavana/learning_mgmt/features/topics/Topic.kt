package dev.fitiavana.learning_mgmt.features.topics

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.fitiavana.learning_mgmt.features.phases.Phase

/**
 * Structure of a topic, an ordered child of a phase. Progress lives in `features/progress`.
 * A topic with a [total] is quantified (e.g. 40 pages); without one it is simple.
 */
@Entity(
    tableName = "topic",
    foreignKeys = [
        ForeignKey(
            entity = Phase::class,
            parentColumns = ["id"],
            childColumns = ["phaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("phaseId")],
)
data class Topic(
    @PrimaryKey val id: String,
    val phaseId: String,
    val number: Int,
    val name: String,
    val total: Int?,
    val unit: String?,
)
