package dev.fitiavana.learning_mgmt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.fitiavana.learning_mgmt.ui.home.HomeScreen
import dev.fitiavana.learning_mgmt.ui.home.HomeViewModel
import dev.fitiavana.learning_mgmt.ui.theme.LearningmgmtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LearningMgmtApplication).container
        setContent {
            LearningmgmtTheme {
                val home: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            HomeViewModel(container.curriculumSelection, container.progressRepository)
                        }
                    },
                )
                val state by home.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onStart = home::startNext,
                    onComplete = home::completeCurrent,
                    onManageCurricula = {}, // Manage curricula arrives in a later slice.
                )
            }
        }
    }
}
