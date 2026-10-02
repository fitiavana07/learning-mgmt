package dev.fitiavana.learning_mgmt.features.phases

/** Structure of a phase. Progress lives in `features/progress`. */
data class Phase(
    val id: String,
    val curriculumId: String,
    val number: Int,
    val name: String,
    val description: String,
)
