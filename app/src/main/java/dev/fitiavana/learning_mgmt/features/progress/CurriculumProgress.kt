package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.phases.Phase

/** Where a curriculum stands: [position] of [total] phases, plus what to say about it. */
data class CurriculumProgress(val position: Int, val total: Int, val summary: Summary) {
    sealed interface Summary {
        data object NoPhases : Summary
        data object NotStarted : Summary
        data class InProgress(val phase: Phase) : Summary
        data class Next(val phase: Phase) : Summary
        data object AllCompleted : Summary
    }

    /** Share of completed phases: the in-progress phase is not counted, unlike [position]. */
    val fraction: Float
        get() {
            if (total == 0) return 0f
            val completed = if (summary is Summary.InProgress) position - 1 else position
            return completed.toFloat() / total
        }

    /** Share of phases completed or in progress, i.e. [position] over [total]. */
    val startedFraction: Float
        get() = if (total == 0) 0f else position.toFloat() / total

    companion object {
        fun of(phases: List<PhaseWithStatus>): CurriculumProgress {
            val current = PhaseRules.current(phases)
            val completed = phases.count { it.status == Status.COMPLETED }
            val next = PhaseRules.nextToStart(phases)
            val summary = when {
                phases.isEmpty() -> Summary.NoPhases
                current != null -> Summary.InProgress(current.phase)
                next == null -> Summary.AllCompleted
                completed == 0 -> Summary.NotStarted
                else -> Summary.Next(next.phase)
            }
            return CurriculumProgress(
                position = current?.phase?.number ?: completed,
                total = phases.size,
                summary = summary,
            )
        }
    }
}
