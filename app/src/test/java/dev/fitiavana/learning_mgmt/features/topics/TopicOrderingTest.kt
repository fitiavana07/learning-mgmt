package dev.fitiavana.learning_mgmt.features.topics

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicOrderingTest {
    private fun topic(id: String, number: Int) =
        Topic(id = id, phaseId = "p", number = number, name = "Name $id", total = null, unit = null)

    private fun List<Topic>.idsByNumber() = map { it.number to it.id }

    @Test
    fun renumberSortsAndClosesGaps() {
        val result = TopicOrdering.renumber(listOf(topic("c", 7), topic("a", 2), topic("b", 5)))

        assertEquals(listOf(1 to "a", 2 to "b", 3 to "c"), result.idsByNumber())
    }

    @Test
    fun nextNumberIsOnePastTheLast() {
        assertEquals(1, TopicOrdering.nextNumber(emptyList()))
        assertEquals(3, TopicOrdering.nextNumber(listOf(topic("a", 1), topic("b", 2))))
    }

    @Test
    fun removeDropsTheTopicAndRenumbersTheRest() {
        val result = TopicOrdering.remove(listOf(topic("a", 1), topic("b", 2), topic("c", 3)), "a")

        assertEquals(listOf(1 to "b", 2 to "c"), result.idsByNumber())
    }

    @Test
    fun moveForwardShiftsTheOthersBack() {
        val result = TopicOrdering.move(listOf(topic("a", 1), topic("b", 2), topic("c", 3)), from = 0, to = 2)

        assertEquals(listOf(1 to "b", 2 to "c", 3 to "a"), result.idsByNumber())
    }
}
