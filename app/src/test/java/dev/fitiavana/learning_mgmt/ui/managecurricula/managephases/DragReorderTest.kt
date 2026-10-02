package dev.fitiavana.learning_mgmt.ui.managecurricula.managephases

import org.junit.Assert.assertEquals
import org.junit.Test

class DragReorderTest {
    private fun target(from: Int, offset: Float, count: Int = 5) =
        dragTargetIndex(from = from, offset = offset, itemHeight = 100f, count = count)

    @Test
    fun staysPutWithoutMovement() = assertEquals(2, target(from = 2, offset = 0f))

    @Test
    fun staysPutBeforeCrossingHalfARow() = assertEquals(2, target(from = 2, offset = 49f))

    @Test
    fun movesDownOnceMoreThanHalfARowIsCrossed() = assertEquals(3, target(from = 2, offset = 51f))

    @Test
    fun movesUpOnceMoreThanHalfARowIsCrossed() = assertEquals(1, target(from = 2, offset = -51f))

    @Test
    fun movesSeveralRows() = assertEquals(4, target(from = 1, offset = 290f))

    @Test
    fun cannotGoPastTheLastRow() = assertEquals(4, target(from = 3, offset = 900f))

    @Test
    fun cannotGoAboveTheFirstRow() = assertEquals(0, target(from = 1, offset = -900f))

    @Test
    fun anEmptyOrUnmeasuredListKeepsThePosition() = assertEquals(2, target(from = 2, offset = 300f, count = 0))
}
