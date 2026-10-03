package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.phases.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseRulesTest {
    private fun p(number: Int, status: Status) = PhaseWithStatus(
        phase = Phase(id = "p$number", curriculumId = "c", number = number, name = "Phase $number", description = ""),
        status = status,
    )

    private val notStarted = Status.NOT_STARTED
    private val inProgress = Status.IN_PROGRESS
    private val completed = Status.COMPLETED

    @Test
    fun currentIsTheInProgressPhase() {
        val phases = listOf(p(1, completed), p(2, inProgress), p(3, notStarted))

        assertEquals(phases[1], PhaseRules.current(phases))
    }

    @Test
    fun currentIsNullWhenNothingIsInProgress() {
        assertNull(PhaseRules.current(listOf(p(1, completed), p(2, notStarted))))
        assertNull(PhaseRules.current(emptyList()))
    }

    @Test
    fun nextToStartIsTheLowestNotStartedPhase() {
        val phases = listOf(p(1, completed), p(2, notStarted), p(3, notStarted))

        assertEquals(phases[1], PhaseRules.nextToStart(phases))
    }

    @Test
    fun nextToStartIgnoresOrderOfCompletedPhases() {
        val phases = listOf(p(1, notStarted), p(2, completed))

        assertEquals(phases[0], PhaseRules.nextToStart(phases))
    }

    @Test
    fun nextToStartIsNullWhileAPhaseIsInProgress() {
        assertNull(PhaseRules.nextToStart(listOf(p(1, inProgress), p(2, notStarted))))
    }

    @Test
    fun nextToStartIsNullWhenEverythingIsCompleted() {
        assertNull(PhaseRules.nextToStart(listOf(p(1, completed), p(2, completed))))
        assertNull(PhaseRules.nextToStart(emptyList()))
    }

    @Test
    fun onlyTheNextToStartPhaseCanBeStarted() {
        val phases = listOf(p(1, completed), p(2, notStarted), p(3, notStarted))

        assertFalse(PhaseRules.canStart(phases, phases[0]))
        assertTrue(PhaseRules.canStart(phases, phases[1]))
        assertFalse(PhaseRules.canStart(phases, phases[2]))
    }

    @Test
    fun noPhaseCanBeStartedWhileAnotherIsInProgress() {
        val phases = listOf(p(1, inProgress), p(2, notStarted))

        assertFalse(PhaseRules.canStart(phases, phases[1]))
    }

    @Test
    fun actionIsCompleteForTheInProgressPhase() {
        val phases = listOf(p(1, completed), p(2, inProgress), p(3, notStarted))

        assertEquals(PhaseAction.Complete, PhaseRules.actionFor(phases, phases[1]))
    }

    @Test
    fun actionIsStartForTheNextPhase() {
        val phases = listOf(p(1, completed), p(2, notStarted), p(3, notStarted))

        assertEquals(PhaseAction.Start, PhaseRules.actionFor(phases, phases[1]))
    }

    @Test
    fun otherPhasesWaitForTheInProgressPhaseToBeCompleted() {
        val phases = listOf(p(1, inProgress), p(2, notStarted), p(3, notStarted))

        assertEquals(PhaseAction.WaitForCompletion(phases[0].phase), PhaseRules.actionFor(phases, phases[1]))
        assertEquals(PhaseAction.WaitForCompletion(phases[0].phase), PhaseRules.actionFor(phases, phases[2]))
    }

    @Test
    fun laterPhasesWaitForTheNextPhaseToBeStartedWhenNothingIsInProgress() {
        val phases = listOf(p(1, completed), p(2, notStarted), p(3, notStarted))

        assertEquals(PhaseAction.WaitForStart(phases[1].phase), PhaseRules.actionFor(phases, phases[2]))
    }

    @Test
    fun completedPhasesHaveNoAction() {
        val phases = listOf(p(1, completed), p(2, notStarted))

        assertEquals(PhaseAction.None, PhaseRules.actionFor(phases, phases[0]))
    }

    @Test
    fun aPhaseCannotBeCompletedWhileItHasUncompletedTopics() {
        val phases = listOf(p(1, inProgress))

        assertFalse(PhaseRules.canComplete(phases, phases[0], TopicSummary(completed = 1, total = 2)))
        assertTrue(PhaseRules.canComplete(phases, phases[0], TopicSummary(completed = 2, total = 2)))
        assertTrue(PhaseRules.canComplete(phases, phases[0], TopicSummary(completed = 0, total = 0)))
    }

    @Test
    fun completeActionOnlyOffersCompletionOnceAllTopicsAreCompleted() {
        assertEquals(PhaseAction.Complete, PhaseRules.completeAction(TopicSummary(completed = 2, total = 2)))
        assertEquals(PhaseAction.Complete, PhaseRules.completeAction(TopicSummary(completed = 0, total = 0)))
        assertEquals(
            PhaseAction.WaitForTopics(TopicSummary(completed = 1, total = 3)),
            PhaseRules.completeAction(TopicSummary(completed = 1, total = 3)),
        )
    }

    @Test
    fun theInProgressPhaseWaitsForItsUncompletedTopics() {
        val phases = listOf(p(1, inProgress), p(2, notStarted))
        val topics = TopicSummary(completed = 1, total = 3)

        assertEquals(PhaseAction.WaitForTopics(topics), PhaseRules.actionFor(phases, phases[0], topics))
        assertEquals(PhaseAction.WaitForCompletion(phases[0].phase), PhaseRules.actionFor(phases, phases[1], topics))
    }

    @Test
    fun onlyTheInProgressPhaseCanBeCompleted() {
        val phases = listOf(p(1, completed), p(2, inProgress), p(3, notStarted))

        assertFalse(PhaseRules.canComplete(phases, phases[0]))
        assertTrue(PhaseRules.canComplete(phases, phases[1]))
        assertFalse(PhaseRules.canComplete(phases, phases[2]))
    }
}
