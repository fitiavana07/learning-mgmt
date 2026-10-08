package dev.fitiavana.learning_mgmt.features.phases

import org.junit.Assert.assertEquals
import org.junit.Test

class PhaseOrderingTest {
    private fun phase(id: String, number: Int) =
        Phase(id = id, curriculumId = "c", number = number, name = "Name $id", description = "")

    private fun List<Phase>.idsByNumber() = map { it.number to it.id }

    @Test
    fun renumberSortsAndClosesGaps() {
        val result = PhaseOrdering.renumber(listOf(phase("c", 7), phase("a", 2), phase("b", 5)))

        assertEquals(listOf(1 to "a", 2 to "b", 3 to "c"), result.idsByNumber())
    }

    @Test
    fun nextNumberIsOnePastTheLast() {
        assertEquals(1, PhaseOrdering.nextNumber(emptyList()))
        assertEquals(3, PhaseOrdering.nextNumber(listOf(phase("a", 1), phase("b", 2))))
    }

    @Test
    fun nextNumberIsOnePastTheHighestEvenWithGapsOrDuplicates() {
        assertEquals(6, PhaseOrdering.nextNumber(listOf(phase("a", 1), phase("b", 5))))
        assertEquals(3, PhaseOrdering.nextNumber(listOf(phase("a", 2), phase("b", 2))))
    }

    @Test
    fun renumberBreaksTiesById() {
        val result = PhaseOrdering.renumber(listOf(phase("b", 1), phase("a", 1), phase("c", 1)))

        assertEquals(listOf(1 to "a", 2 to "b", 3 to "c"), result.idsByNumber())
    }

    @Test
    fun moveUsesTheSameOrderAsTheListsShown() {
        val result = PhaseOrdering.move(listOf(phase("b", 1), phase("a", 1), phase("c", 2)), from = 0, to = 2)

        assertEquals(listOf(1 to "b", 2 to "c", 3 to "a"), result.idsByNumber().sortedBy { it.first })
    }

    @Test
    fun removeDropsThePhaseAndRenumbersTheRest() {
        val result = PhaseOrdering.remove(listOf(phase("a", 1), phase("b", 2), phase("c", 3)), "a")

        assertEquals(listOf(1 to "b", 2 to "c"), result.idsByNumber())
    }

    @Test
    fun moveForwardShiftsTheOthersBack() {
        val result = PhaseOrdering.move(listOf(phase("a", 1), phase("b", 2), phase("c", 3)), from = 0, to = 2)

        assertEquals(listOf(1 to "b", 2 to "c", 3 to "a"), result.idsByNumber())
    }

    @Test
    fun moveBackwardShiftsTheOthersForward() {
        val result = PhaseOrdering.move(listOf(phase("a", 1), phase("b", 2), phase("c", 3)), from = 2, to = 0)

        assertEquals(listOf(1 to "c", 2 to "a", 3 to "b"), result.idsByNumber())
    }

    @Test
    fun moveToSamePositionChangesNothing() {
        val phases = listOf(phase("a", 1), phase("b", 2))

        assertEquals(phases, PhaseOrdering.move(phases, from = 1, to = 1))
    }

    @Test
    fun moveKeepsEachPhaseIdentity() {
        val phases = listOf(phase("a", 1), phase("b", 2))

        val moved = PhaseOrdering.move(phases, from = 0, to = 1)

        assertEquals(phase("b", 1), moved[0])
        assertEquals(phase("a", 2), moved[1])
    }
}
