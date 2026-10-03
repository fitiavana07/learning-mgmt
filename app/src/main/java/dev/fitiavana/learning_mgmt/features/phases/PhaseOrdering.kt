package dev.fitiavana.learning_mgmt.features.phases

import dev.fitiavana.learning_mgmt.features.NumberedOrdering

/** Keeps phase numbers contiguous (1..N) and equal to their order. */
val PhaseOrdering = NumberedOrdering<Phase>(
    id = { it.id },
    number = { it.number },
    withNumber = { phase, number -> phase.copy(number = number) },
)
