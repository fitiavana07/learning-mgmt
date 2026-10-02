package dev.fitiavana.learning_mgmt.features.progress

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.fitiavana.learning_mgmt.features.phases.Phase

/** Progress of one phase. A phase without a row is NOT_STARTED. */
@Entity(
    tableName = "phase_status",
    foreignKeys = [
        ForeignKey(
            entity = Phase::class,
            parentColumns = ["id"],
            childColumns = ["phaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PhaseStatus(
    @PrimaryKey val phaseId: String,
    val status: Status,
)

/** A phase joined with its optional status row. */
data class PhaseStatusRow(
    @Embedded val phase: Phase,
    val status: Status?,
) {
    fun toPhaseWithStatus() = PhaseWithStatus(phase, status ?: Status.NOT_STARTED)
}
