package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.backup.BackupData

/**
 * Everything one device knows: all its rows, plus the stamp of every row and the tombstone of
 * every deleted one. This is what two devices exchange; the selected curriculum stays out of it.
 */
data class SyncSnapshot(val data: BackupData, val meta: Map<RecordKey, Meta>) {
    /**
     * The first inconsistency between the rows and their stamps, in plain language, or null; a
     * failure means a corrupt or foreign peer and the snapshot is not used. Rules between rows
     * (one phase in progress...) are not checked here: a device can hold such a state itself, and
     * the receiver repairs it after merging.
     */
    fun violation(): String? {
        val rows = data.recordKeys()
        rows.firstOrNull { meta[it] == null }
            ?.let { return "A ${it.kind.label()} has no stamp" }
        rows.firstOrNull { meta.getValue(it).deleted }
            ?.let { return "A ${it.kind.label()} is both present and marked deleted" }
        meta.entries.firstOrNull { !it.value.deleted && it.key !in rows }
            ?.let { return "A stamp refers to a ${it.key.kind.label()} that has no row" }
        return null
    }

    private fun SyncKind.label() = name.lowercase().replace('_', ' ')
}
