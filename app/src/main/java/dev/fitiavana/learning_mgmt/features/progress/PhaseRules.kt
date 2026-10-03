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

    /** The in-progress phase, once all its [topics] (none by default) are completed. */
    fun canComplete(
        phases: List<PhaseWithStatus>,
        target: PhaseWithStatus,
        topics: TopicSummary = TopicSummary.None,
    ): Boolean = current(phases)?.phase?.id == target.phase.id && topics.allCompleted

    /** Completing the in-progress phase, or waiting for its [topics] to be completed first. */
    fun completeAction(topics: TopicSummary): PhaseAction =
        if (topics.allCompleted) PhaseAction.Complete else PhaseAction.WaitForTopics(topics)

    fun actionFor(
        phases: List<PhaseWithStatus>,
        target: PhaseWithStatus,
        topics: TopicSummary = TopicSummary.None,
    ): PhaseAction {
        val current = current(phases)
        val next = nextToStart(phases)
        return when {
            canComplete(phases, target) -> completeAction(topics)
            canStart(phases, target) -> PhaseAction.Start
            target.status != Status.NOT_STARTED -> PhaseAction.None
            current != null -> PhaseAction.WaitForCompletion(current.phase)
            next != null -> PhaseAction.WaitForStart(next.phase)
            else -> PhaseAction.None
        }
    }
}
