package dev.fitiavana.learning_mgmt.ui

import android.view.View
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.isRoot
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Pretends the soft keyboard is open with the given height, and returns the y below which nothing is visible. */
fun ComposeContentTestRule.showKeyboard(view: View, heightPx: Int): Float {
    runOnUiThread {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, heightPx))
            .setVisible(WindowInsetsCompat.Type.ime(), true)
            .build()
        ViewCompat.dispatchApplyWindowInsets(view, insets)
    }
    waitForIdle()
    // A focused text field can add a popup root; the first one is the main window.
    return onAllNodes(isRoot()).fetchSemanticsNodes().first().boundsInRoot.bottom - heightPx
}
