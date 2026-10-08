package dev.fitiavana.learning_mgmt

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** Runs something only while the app is on screen: sync does not run in the background. */
class ForegroundObserver(
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = onForeground()

    override fun onStop(owner: LifecycleOwner) = onBackground()
}
