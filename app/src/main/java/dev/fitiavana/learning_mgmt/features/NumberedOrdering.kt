package dev.fitiavana.learning_mgmt.features

/** Keeps the numbers of an ordered list of [T] contiguous (1..N) and equal to their order. */
class NumberedOrdering<T>(
    private val id: (T) -> String,
    private val number: (T) -> Int,
    private val withNumber: (T, Int) -> T,
) {
    fun renumber(items: List<T>): List<T> = numberInOrder(items.sortedBy(number))

    private fun numberInOrder(ordered: List<T>): List<T> =
        ordered.mapIndexed { index, item -> withNumber(item, index + 1) }

    fun nextNumber(items: List<T>): Int = items.size + 1

    fun remove(items: List<T>, id: String): List<T> =
        renumber(items.filterNot { id(it) == id })

    /** Moves the item at position [from] to position [to] (both 0-based, in number order). */
    fun move(items: List<T>, from: Int, to: Int): List<T> {
        val reordered = items.sortedBy(number).toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return numberInOrder(reordered)
    }
}
