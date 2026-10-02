package dev.fitiavana.learning_mgmt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.fitiavana.learning_mgmt.ui.home.HomeRoute
import dev.fitiavana.learning_mgmt.ui.home.HomeViewModel
import dev.fitiavana.learning_mgmt.ui.home.drawer.DrawerViewModel
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
                        initializer { HomeViewModel(container.curriculumSelection, container.progressRepository) }
                    },
                )
                val drawer: DrawerViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            DrawerViewModel(
                                container.curriculumRepository,
                                container.curriculumSelection,
                                container.progressRepository,
                            )
                        }
                    },
                )
                HomeRoute(
                    home = home,
                    drawer = drawer,
                    onManageCurricula = {}, // Manage curricula arrives in a later slice.
                )
            }
        }
    }
}
