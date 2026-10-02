package dev.fitiavana.learning_mgmt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.fitiavana.learning_mgmt.AppContainer
import dev.fitiavana.learning_mgmt.ui.home.HomeRoute
import dev.fitiavana.learning_mgmt.ui.home.HomeViewModel
import dev.fitiavana.learning_mgmt.ui.home.drawer.DrawerViewModel
import dev.fitiavana.learning_mgmt.ui.home.phases.PhaseViewModel
import dev.fitiavana.learning_mgmt.ui.home.phases.PhaseViewScreen
import dev.fitiavana.learning_mgmt.ui.home.phases.PhasesScreen
import dev.fitiavana.learning_mgmt.ui.home.phases.PhasesViewModel

private object Routes {
    const val HOME = "home"
    const val PHASES = "phases"
    const val PHASE_ID = "phaseId"
    const val PHASE = "phases/{$PHASE_ID}"
    fun phase(id: String) = "phases/$id"
}

/** The navigation graph; each destination gets its ViewModel from [container]. */
@Composable
fun AppNavHost(container: AppContainer, onManageCurricula: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
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
                onManageCurricula = onManageCurricula,
                onShowPhases = { navController.navigate(Routes.PHASES) },
            )
        }
        composable(Routes.PHASES) {
            val phases: PhasesViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { PhasesViewModel(container.curriculumSelection, container.progressRepository) }
                },
            )
            val state by phases.uiState.collectAsStateWithLifecycle()
            PhasesScreen(
                state = state,
                onPhaseClick = { navController.navigate(Routes.phase(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PHASE, arguments = listOf(navArgument(Routes.PHASE_ID) { type = NavType.StringType })) { entry ->
            val phaseId = checkNotNull(entry.arguments?.getString(Routes.PHASE_ID))
            val phase: PhaseViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        PhaseViewModel(phaseId, container.curriculumSelection, container.progressRepository)
                    }
                },
            )
            val state by phase.uiState.collectAsStateWithLifecycle()
            PhaseViewScreen(
                state = state,
                onStart = phase::start,
                onComplete = phase::complete,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
