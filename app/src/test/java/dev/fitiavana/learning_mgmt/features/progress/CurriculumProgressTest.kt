package dev.fitiavana.learning_mgmt.features.progress

import dev.fitiavana.learning_mgmt.features.phases.Phase
import org.junit.Assert.assertEquals
import org.junit.Test

class CurriculumProgressTest {
    private fun p(number: Int, status: Status) = PhaseWithStatus(
        phase = Phase(id = "p$number", curriculumId = "c", number = number, name = "Phase $number", description = ""),
        status = status,
    )

    private val notStarted = Status.NOT_STARTED
    private val inProgress = Status.IN_PROGRESS
    private val completed = Status.COMPLETED

    @Test
    fun noPhases() {
        val progress = CurriculumProgress.of(emptyList())

        assertEquals(0, progress.position)
        assertEquals(0, progress.total)
        assertEquals(CurriculumProgress.Summary.NoPhases, progress.summary)
    }

    @Test
    fun nothingStarted() {
        val progress = CurriculumProgress.of(listOf(p(1, notStarted), p(2, notStarted)))

        assertEquals(0, progress.position)
        assertEquals(2, progress.total)
        assertEquals(CurriculumProgress.Summary.NotStarted, progress.summary)
    }

    @Test
    fun positionIsTheInProgressPhaseNumber() {
        val phases = listOf(p(1, completed), p(2, inProgress), p(3, notStarted))

        val progress = CurriculumProgress.of(phases)

        assertEquals(2, progress.position)
        assertEquals(3, progress.total)
        assertEquals(CurriculumProgress.Summary.InProgress(phases[1].phase), progress.summary)
    }

    @Test
    fun betweenPhasesPositionIsTheCompletedCountAndNextIsOffered() {
        val phases = listOf(p(1, completed), p(2, completed), p(3, notStarted), p(4, notStarted))

        val progress = CurriculumProgress.of(phases)

        assertEquals(2, progress.position)
        assertEquals(4, progress.total)
        assertEquals(CurriculumProgress.Summary.Next(phases[2].phase), progress.summary)
    }

    @Test
    fun allCompleted() {
        val progress = CurriculumProgress.of(listOf(p(1, completed), p(2, completed)))

        assertEquals(2, progress.position)
        assertEquals(2, progress.total)
        assertEquals(CurriculumProgress.Summary.AllCompleted, progress.summary)
    }

    @Test
    fun fractionCountsOnlyCompletedPhases() {
        val phases = listOf(p(1, completed), p(2, inProgress), p(3, notStarted))

        assertEquals(1f / 3, CurriculumProgress.of(phases).fraction, 0.001f)
    }

    @Test
    fun fractionIsEmptyWhenTheFirstPhaseIsInProgress() {
        val progress = CurriculumProgress.of(listOf(p(1, inProgress), p(2, notStarted)))

        assertEquals(0f, progress.fraction, 0.001f)
    }

    @Test
    fun lastPhaseInProgressIsNotFull() {
        val progress = CurriculumProgress.of(listOf(p(1, completed), p(2, inProgress)))

        assertEquals(0.5f, progress.fraction, 0.001f)
    }

    @Test
    fun fractionIsFullOnlyWhenAllCompleted() {
        val progress = CurriculumProgress.of(listOf(p(1, completed), p(2, completed)))

        assertEquals(1f, progress.fraction, 0.001f)
    }

    @Test
    fun startedFractionIncludesTheInProgressPhase() {
        val phases = listOf(p(1, completed), p(2, inProgress), p(3, notStarted))

        assertEquals(2f / 3, CurriculumProgress.of(phases).startedFraction, 0.001f)
    }

    @Test
    fun startedFractionEqualsFractionWithoutAPhaseInProgress() {
        val between = CurriculumProgress.of(listOf(p(1, completed), p(2, notStarted)))

        assertEquals(between.fraction, between.startedFraction, 0.001f)
        assertEquals(0f, CurriculumProgress.of(emptyList()).startedFraction, 0.001f)
    }

    @Test
    fun fractionIsZeroWithoutPhases() {
        assertEquals(0f, CurriculumProgress.of(emptyList()).fraction, 0.001f)
    }
}
