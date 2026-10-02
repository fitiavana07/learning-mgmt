package dev.fitiavana.learning_mgmt.features.progress

/** Start/complete rules for the phases of one curriculum. Phases are expected in number order. */
object PhaseRules {
    fun current(phases: List<PhaseWithStatus>): PhaseWithStatus? =
        phases.firstOrNull { it.status == Status.IN_PROGRESS }

    /** The lowest-numbered not-started phase, and only while nothing is in progress. */
    fun nextToStart(phases: List<PhaseWithStatus>): PhaseWithStatus? =
        if (current(phases) != null) null
        else phases.filter { it.status == Status.NOT_STARTED }.minByOrNull { it.phase.number }

    fun canStart(phases: List<PhaseWithStatus>, target: PhaseWithStatus): Boolean =
        nextToStart(phases)?.phase?.id == target.phase.id

    fun canComplete(phases: List<PhaseWithStatus>, target: PhaseWithStatus): Boolean =
        current(phases)?.phase?.id == target.phase.id
}
