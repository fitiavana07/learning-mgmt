package dev.fitiavana.learning_mgmt.features

/**
 * Orders a list of [T] by number, ties broken by id (a sync can leave duplicates and gaps), and
 * renumbers it contiguously (1..N) when it is changed.
 */
class NumberedOrdering<T>(
    private val id: (T) -> String,
    private val number: (T) -> Int,
    private val withNumber: (T, Int) -> T,
) {
    private fun inOrder(items: List<T>): List<T> = items.sortedWith(compareBy(number, id))

    fun renumber(items: List<T>): List<T> = numberInOrder(inOrder(items))

    private fun numberInOrder(ordered: List<T>): List<T> =
        ordered.mapIndexed { index, item -> withNumber(item, index + 1) }

    /** One past the highest number, so an addition never collides with a gap. */
    fun nextNumber(items: List<T>): Int = (items.maxOfOrNull(number) ?: 0) + 1

    fun remove(items: List<T>, id: String): List<T> =
        renumber(items.filterNot { id(it) == id })

    /** Moves the item at position [from] to position [to] (both 0-based, in number order). */
    fun move(items: List<T>, from: Int, to: Int): List<T> {
        val reordered = inOrder(items).toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return numberInOrder(reordered)
    }
}
