package dev.fitiavana.learning_mgmt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.fitiavana.learning_mgmt.ui.AppNavHost
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LearningMgmtApplication).container
        setContent {
            LearningmgmtTheme {
                AppNavHost(container)
            }
        }
    }
}
