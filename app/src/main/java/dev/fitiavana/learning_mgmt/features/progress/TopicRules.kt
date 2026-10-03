package dev.fitiavana.learning_mgmt.features.progress

/** Completed topics out of all topics of a phase. A phase without topics counts as all completed. */
data class TopicSummary(val completed: Int, val total: Int) {
    val allCompleted: Boolean get() = completed == total
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total

    companion object {
        val None = TopicSummary(completed = 0, total = 0)
    }
}

/** Start/complete/progress rules for the topics of one phase. Topics are expected in number order. */
object TopicRules {
    fun current(topics: List<TopicWithProgress>): TopicWithProgress? =
        topics.firstOrNull { it.status == Status.IN_PROGRESS }

    /** The lowest-numbered not-started topic, and only while nothing is in progress. */
    fun nextToStart(topics: List<TopicWithProgress>): TopicWithProgress? =
        if (current(topics) != null) null
        else topics.filter { it.status == Status.NOT_STARTED }.minByOrNull { it.topic.number }

    fun canStart(topics: List<TopicWithProgress>, target: TopicWithProgress): Boolean =
        nextToStart(topics)?.topic?.id == target.topic.id

    fun canComplete(topics: List<TopicWithProgress>, target: TopicWithProgress): Boolean =
        current(topics)?.topic?.id == target.topic.id

    /** Records [done] on a quantified topic, clamped to 0..total; reaching the total completes it. */
    fun record(topic: TopicWithProgress, done: Int): TopicWithProgress {
        val total = topic.topic.total ?: return topic
        val clamped = done.coerceIn(0, total)
        return topic.copy(
            status = if (clamped == total) Status.COMPLETED else topic.status,
            done = clamped,
        )
    }

    /** One more than [current] (a blank entry counts as 0), capped at [total]. */
    fun increase(current: Int?, total: Int): Int = ((current ?: 0) + 1).coerceAtMost(total)

    /** Changes the total of a quantified topic, clamping the recorded progress to it. */
    fun withTotal(topic: TopicWithProgress, total: Int): TopicWithProgress =
        record(topic.copy(topic = topic.topic.copy(total = total)), topic.done)

    fun fraction(topic: TopicWithProgress): Float {
        val total = topic.topic.total
        return when {
            topic.status == Status.COMPLETED -> 1f
            total != null -> topic.done.toFloat() / total
            else -> 0f
        }
    }

    fun summary(topics: List<TopicWithProgress>): TopicSummary =
        TopicSummary(completed = topics.count { it.status == Status.COMPLETED }, total = topics.size)
}
