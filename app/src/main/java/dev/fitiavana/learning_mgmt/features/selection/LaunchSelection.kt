package dev.fitiavana.learning_mgmt.features.selection

object LaunchSelection {
    /** The stored id if it still exists, else the first available one, else none. */
    fun resolve(storedId: String?, availableIds: List<String>): String? =
        availableIds.firstOrNull { it == storedId } ?: availableIds.firstOrNull()
}
