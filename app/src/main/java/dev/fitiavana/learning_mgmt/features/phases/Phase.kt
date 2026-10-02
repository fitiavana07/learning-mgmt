package dev.fitiavana.learning_mgmt.features.phases

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.fitiavana.learning_mgmt.features.curricula.Curriculum

/** Structure of a phase. Progress lives in `features/progress`. */
@Entity(
    tableName = "phase",
    foreignKeys = [
        ForeignKey(
            entity = Curriculum::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("curriculumId")],
)
data class Phase(
    @PrimaryKey val id: String,
    val curriculumId: String,
    val number: Int,
    val name: String,
    val description: String,
)
