package dev.fitiavana.learning_mgmt.features.topics

import dev.fitiavana.learning_mgmt.features.NumberedOrdering

/** Keeps topic numbers contiguous (1..N) and equal to their order within a phase. */
val TopicOrdering = NumberedOrdering<Topic>(
    id = { it.id },
    number = { it.number },
    withNumber = { topic, number -> topic.copy(number = number) },
)
