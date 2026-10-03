package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.phases.Phase

/** What the user can do with a phase right now. */
sealed interface PhaseAction {
    data object Complete : PhaseAction
    data object Start : PhaseAction

    /** Not startable until [phase], currently in progress, is completed. */
    data class WaitForCompletion(val phase: Phase) : PhaseAction

    /** Not startable until the earlier not-started [phase] is started and completed. */
    data class WaitForStart(val phase: Phase) : PhaseAction

    /** The in-progress phase cannot be completed until all its topics (counted in [topics]) are. */
    data class WaitForTopics(val topics: TopicSummary) : PhaseAction

    data object None : PhaseAction
}
