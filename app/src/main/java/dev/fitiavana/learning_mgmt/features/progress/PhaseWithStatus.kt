package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.phases.Phase

/** Read model joining a phase's structure with its progress. */
data class PhaseWithStatus(val phase: Phase, val status: Status)
