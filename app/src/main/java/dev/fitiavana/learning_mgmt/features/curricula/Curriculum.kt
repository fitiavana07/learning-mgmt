package dev.fitiavana.learning_mgmt.features.curricula

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "curriculum")
data class Curriculum(
    @PrimaryKey val id: String,
    val name: String,
)
