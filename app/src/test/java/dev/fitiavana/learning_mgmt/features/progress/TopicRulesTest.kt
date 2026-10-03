package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.topics.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicRulesTest {
    private fun t(number: Int, status: Status, total: Int? = null, done: Int = 0) = TopicWithProgress(
        topic = Topic(id = "t$number", phaseId = "p", number = number, name = "Topic $number", total = total, unit = null),
        status = status,
        done = done,
    )

    private val notStarted = Status.NOT_STARTED
    private val inProgress = Status.IN_PROGRESS
    private val completed = Status.COMPLETED

    @Test
    fun currentIsTheInProgressTopic() {
        val topics = listOf(t(1, completed), t(2, inProgress), t(3, notStarted))

        assertEquals(topics[1], TopicRules.current(topics))
    }

    @Test
    fun nextToStartIsTheLowestNotStartedTopicWhileNothingIsInProgress() {
        val topics = listOf(t(1, completed), t(2, notStarted), t(3, notStarted))

        assertEquals(topics[1], TopicRules.nextToStart(topics))
        assertNull(TopicRules.nextToStart(listOf(t(1, inProgress), t(2, notStarted))))
    }

    @Test
    fun onlyTheNextTopicCanBeStartedAndOnlyTheCurrentOneCompleted() {
        val topics = listOf(t(1, inProgress), t(2, notStarted))

        assertTrue(TopicRules.canComplete(topics, topics[0]))
        assertFalse(TopicRules.canStart(topics, topics[1]))
        assertFalse(TopicRules.canComplete(topics, topics[1]))
    }

    @Test
    fun recordingProgressBelowTheTotalKeepsTheTopicInProgress() {
        val topic = t(1, inProgress, total = 40, done = 10)

        assertEquals(topic.copy(done = 25), TopicRules.record(topic, 25))
    }

    @Test
    fun recordingTheTotalCompletesTheTopic() {
        val topic = t(1, inProgress, total = 40, done = 10)

        assertEquals(topic.copy(status = completed, done = 40), TopicRules.record(topic, 40))
    }

    @Test
    fun recordedProgressIsClampedBetweenZeroAndTheTotal() {
        val topic = t(1, inProgress, total = 40)

        assertEquals(40, TopicRules.record(topic, 99).done)
        assertEquals(0, TopicRules.record(topic, -3).done)
    }

    @Test
    fun increasingAddsOneUpToTheTotal() {
        assertEquals(13, TopicRules.increase(12, total = 40))
        assertEquals(40, TopicRules.increase(39, total = 40))
        assertEquals(40, TopicRules.increase(40, total = 40))
        assertEquals(40, TopicRules.increase(95, total = 40))
    }

    @Test
    fun increasingABlankEntryStartsFromZero() {
        assertEquals(1, TopicRules.increase(null, total = 40))
    }

    @Test
    fun progressCannotBeRecordedOnASimpleTopic() {
        val topic = t(1, inProgress)

        assertEquals(topic, TopicRules.record(topic, 5))
    }

    @Test
    fun fractionIsDoneOverTotalForQuantifiedTopicsAndStatusBasedForSimpleOnes() {
        assertEquals(0.25f, TopicRules.fraction(t(1, inProgress, total = 40, done = 10)), 0.0001f)
        assertEquals(1f, TopicRules.fraction(t(1, completed)), 0f)
        assertEquals(0f, TopicRules.fraction(t(1, inProgress)), 0f)
    }

    @Test
    fun summaryCountsCompletedTopicsOverAllTopics() {
        val summary = TopicRules.summary(listOf(t(1, completed), t(2, inProgress), t(3, notStarted)))

        assertEquals(1, summary.completed)
        assertEquals(3, summary.total)
        assertFalse(summary.allCompleted)
    }

    @Test
    fun aPhaseWithoutTopicsHasNothingBlockingItsCompletion() {
        assertTrue(TopicRules.summary(emptyList()).allCompleted)
    }

    @Test
    fun aPhaseWithAllTopicsCompletedHasNothingBlockingItsCompletion() {
        assertTrue(TopicRules.summary(listOf(t(1, completed), t(2, completed))).allCompleted)
    }

    @Test
    fun loweringTheTotalClampsProgressAndCompletesAFullyDoneTopic() {
        val topic = t(1, inProgress, total = 40, done = 30)

        val lowered = TopicRules.withTotal(topic, 20)

        assertEquals(20, lowered.topic.total)
        assertEquals(20, lowered.done)
        assertEquals(completed, lowered.status)
    }

    @Test
    fun aCompletedQuantifiedTopicIsFullEvenIfItsTotalWasRaisedLater() {
        assertEquals(1f, TopicRules.fraction(t(1, completed, total = 60, done = 40)), 0f)
    }
}
