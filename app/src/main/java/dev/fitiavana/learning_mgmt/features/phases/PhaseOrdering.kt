package dev.fitiavana.learning_mgmt.features.phases

/** Keeps phase numbers contiguous (1..N) and equal to their order. */
object PhaseOrdering {
    fun renumber(phases: List<Phase>): List<Phase> =
        numberInOrder(phases.sortedBy { it.number })

    private fun numberInOrder(ordered: List<Phase>): List<Phase> =
        ordered.mapIndexed { index, phase -> phase.copy(number = index + 1) }

    fun nextNumber(phases: List<Phase>): Int = phases.size + 1

    fun remove(phases: List<Phase>, id: String): List<Phase> =
        renumber(phases.filterNot { it.id == id })

    /** Moves the phase at position [from] to position [to] (both 0-based, in number order). */
    fun move(phases: List<Phase>, from: Int, to: Int): List<Phase> {
        val reordered = phases.sortedBy { it.number }.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return numberInOrder(reordered)
    }
}
