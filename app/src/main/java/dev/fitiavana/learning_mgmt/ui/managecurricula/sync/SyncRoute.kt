package dev.fitiavana.learning_mgmt.ui.managecurricula.sync

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Local network access became a runtime permission with Android 17 (API 37); before it is implicit. */
private const val LOCAL_NETWORK_API = 37
private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

private fun localNetworkAllowed(context: Context) =
    Build.VERSION.SDK_INT < LOCAL_NETWORK_API ||
        ContextCompat.checkSelfPermission(context, LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

/**
 * Connects [SyncScreen] to its [SyncViewModel] and to the local network permission: it is asked
 * for here, when the user opens sync, and checked again on return in case it was changed in settings.
 */
@Composable
fun SyncRoute(viewModel: SyncViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(localNetworkAllowed(context)) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        // The network may have failed to open without it: try again now.
        if (granted) viewModel.refresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = localNetworkAllowed(context) }

    SyncScreen(
        state = state,
        localNetworkBlocked = !allowed,
        onAllowLocalNetwork = { request.launch(LOCAL_NETWORK_PERMISSION) },
        onSavePassphrase = viewModel::savePassphrase,
        onRemovePassphrase = viewModel::removePassphrase,
        onAutoSyncChange = viewModel::setAutoSync,
        onRefresh = viewModel::refresh,
        onSyncWith = viewModel::syncWith,
        onSyncAll = viewModel::syncAll,
        onBack = onBack,
    )
}
