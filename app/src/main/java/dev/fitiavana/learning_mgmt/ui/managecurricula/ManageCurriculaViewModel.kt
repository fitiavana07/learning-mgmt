package dev.fitiavana.learning_mgmt.ui.managecurricula

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fitiavana.learning_mgmt.features.backup.BackupCounts
import dev.fitiavana.learning_mgmt.features.backup.BackupRepository
import dev.fitiavana.learning_mgmt.features.backup.BackupSummary
import dev.fitiavana.learning_mgmt.features.backup.Inspection
import dev.fitiavana.learning_mgmt.features.backup.RestoreResult
import dev.fitiavana.learning_mgmt.features.curricula.CurriculumRepository
import dev.fitiavana.learning_mgmt.features.progress.ProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CurriculumRow(val id: String, val name: String, val phaseCount: Int)

/** A restore waiting for the user's confirmation, with what the file holds and what would be erased. */
data class PendingRestore(val json: String, val summary: BackupSummary, val current: BackupCounts)

/** One-shot outcomes of a backup or restore, shown once (e.g. in a snackbar). */
sealed interface BackupMessage {
    data object BackupSaved : BackupMessage
    data object BackupFailed : BackupMessage
    data object FileUnreadable : BackupMessage
    data object RestoreComplete : BackupMessage
    data class RestoreRejected(val failure: RestoreResult.Failure) : BackupMessage
}

class ManageCurriculaViewModel(
    private val curricula: CurriculumRepository,
    progress: ProgressRepository,
    private val backups: BackupRepository,
) : ViewModel() {
    val rows: StateFlow<List<CurriculumRow>> =
        combine(curricula.observeAll(), progress.observeAll()) { all, phasesByCurriculum ->
            all.map { CurriculumRow(it.id, it.name, phasesByCurriculum[it.id].orEmpty().size) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)

    /** True while a backup is being written or a file is being read or restored. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

    private val messageChannel = Channel<BackupMessage>(Channel.BUFFERED)
    val messages: Flow<BackupMessage> = messageChannel.receiveAsFlow()

    fun create(name: String) {
        viewModelScope.launch { curricula.create(name) }
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { curricula.rename(id, name) }
    }

    fun delete(id: String) {
        viewModelScope.launch { curricula.delete(id) }
    }

    /** Hands the exported JSON to [write], which saves it wherever the user picked. */
    fun backup(write: suspend (String) -> Unit) = runBusy {
        catching { write(backups.export()) }.fold(
            onSuccess = { BackupMessage.BackupSaved },
            onFailure = { BackupMessage.BackupFailed },
        )
    }

    /** Reads a file with [read] and, if it is a valid backup, asks for confirmation before replacing data. */
    fun restoreFrom(read: suspend () -> String) = runBusy {
        val json = catching(read).getOrElse { return@runBusy BackupMessage.FileUnreadable }
        when (val inspection = backups.inspect(json)) {
            is Inspection.Rejected -> BackupMessage.RestoreRejected(inspection.failure)
            is Inspection.Valid -> {
                _pendingRestore.value = PendingRestore(json, inspection.summary, backups.currentCounts())
                null
            }
        }
    }

    fun confirmRestore() {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        runBusy {
            when (val result = backups.restore(pending.json)) {
                RestoreResult.Success -> BackupMessage.RestoreComplete
                is RestoreResult.Failure -> BackupMessage.RestoreRejected(result)
            }
        }
    }

    fun dismissRestore() {
        _pendingRestore.value = null
    }

    /** Runs [block] as the one operation in flight; its result, if any, is sent after `busy` clears. */
    private fun runBusy(block: suspend () -> BackupMessage?) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val message = try {
                block()
            } finally {
                _busy.value = false
            }
            message?.let { messageChannel.send(it) }
        }
    }

    private suspend fun <T> catching(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
