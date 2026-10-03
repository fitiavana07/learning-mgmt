package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.topics.Topic

/** Read model joining a topic's structure with its progress. [done] only matters for quantified topics. */
data class TopicWithProgress(val topic: Topic, val status: Status, val done: Int)
