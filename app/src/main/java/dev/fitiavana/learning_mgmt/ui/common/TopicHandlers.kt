package dev.fitiavana.learning_mgmt.ui.common

import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What the user can do to the topics of the phase in progress, by topic id. */
class TopicHandlers(
    val onStart: (String) -> Unit,
    val onComplete: (String) -> Unit,
    val onRecord: (id: String, done: Int) -> Unit,
) {
    companion object {
        val None = TopicHandlers(onStart = {}, onComplete = {}, onRecord = { _, _ -> })

        /**
         * Handlers that run on [scope]. A tap on a stale row (the topic already moved on, e.g. a
         * double tap) is rejected by the repository and ignored here rather than crashing.
         */
        fun of(scope: CoroutineScope, progress: ProgressRepository) = TopicHandlers(
            onStart = { id -> scope.launchIgnoringStale { progress.startTopic(id) } },
            onComplete = { id -> scope.launchIgnoringStale { progress.completeTopic(id) } },
            onRecord = { id, done -> scope.launchIgnoringStale { progress.recordTopicProgress(id, done) } },
        )

        private fun CoroutineScope.launchIgnoringStale(block: suspend () -> Unit) {
            launch {
                try {
                    block()
                } catch (_: IllegalStateException) {
                    // The repository refused a transition that is no longer allowed.
                }
            }
        }
    }
}
