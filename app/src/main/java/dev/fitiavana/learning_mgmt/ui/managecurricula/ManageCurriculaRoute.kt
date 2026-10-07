package dev.fitiavana.learning_mgmt.ui.managecurricula

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitiavana.learning_mgmt.features.backup.backupFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime

private const val JSON = "application/json"

/**
 * Connects [ManageCurriculaScreen] to its [ManageCurriculaViewModel] and to the system file
 * pickers (no storage permission needed): the save picker for a backup, the open picker for a
 * restore. The ViewModel only sees strings, so it stays free of Android file IO.
 */
@Composable
fun ManageCurriculaRoute(
    viewModel: ManageCurriculaViewModel,
    onCurriculumClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        if (uri != null) {
            viewModel.backup { json ->
                withContext(Dispatchers.IO) {
                    val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
                    out.use { it.write(json.toByteArray()) }
                }
            }
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.restoreFrom {
                withContext(Dispatchers.IO) {
                    val input = resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
                    input.use { it.readBytes().decodeToString() }
                }
            }
        }
    }

    ManageCurriculaScreen(
        rows = rows,
        busy = busy,
        pendingRestore = pendingRestore,
        messages = viewModel.messages,
        onCreate = viewModel::create,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        onCurriculumClick = onCurriculumClick,
        onBackup = { saveBackup.launch(backupFileName(LocalDateTime.now())) },
        onRestore = { pickBackup.launch(arrayOf(JSON, "text/plain", "application/octet-stream")) },
        onConfirmRestore = viewModel::confirmRestore,
        onDismissRestore = viewModel::dismissRestore,
        onBack = onBack,
    )
}
