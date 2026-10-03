package dev.fitiavana.learning_mgmt.ui.common

import kotlin.math.roundToInt

/**
 * Where a row dragged from position [from] by [offset] pixels lands, for [count] rows of equal
 * [itemHeight]. The row moves once it has crossed half of a neighbour, and never leaves the list.
 */
fun dragTargetIndex(from: Int, offset: Float, itemHeight: Float, count: Int): Int {
    if (count <= 0 || itemHeight <= 0f) return from
    return (from + (offset / itemHeight).roundToInt()).coerceIn(0, count - 1)
}
