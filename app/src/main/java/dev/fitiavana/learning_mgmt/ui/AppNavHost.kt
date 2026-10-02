package dev.fitiavana.learning_mgmt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
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
import dev.fitiavana.learning_mgmt.ui.managecurricula.ManageCurriculaScreen
import dev.fitiavana.learning_mgmt.ui.managecurricula.ManageCurriculaViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.ManagePhasesScreen
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.ManagePhasesViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor.ManagedPhaseViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor.PhaseEditorRoute

private object Routes {
    const val HOME = "home"
    const val PHASES = "phases"
    const val PHASE_ID = "phaseId"
    const val PHASE = "phases/{$PHASE_ID}"
    fun phase(id: String) = "phases/$id"

    const val MANAGE_CURRICULA = "manage-curricula"
    const val CURRICULUM_ID = "curriculumId"
    const val MANAGE_PHASES = "$MANAGE_CURRICULA/{$CURRICULUM_ID}"
    fun managePhases(curriculumId: String) = "$MANAGE_CURRICULA/$curriculumId"

    const val MANAGED_PHASE = "$MANAGE_PHASES/phases/{$PHASE_ID}"
    const val EDIT_PHASE = "$MANAGED_PHASE/edit"
    const val NEW_PHASE = "$MANAGE_PHASES/new-phase"
    fun managedPhase(curriculumId: String, phaseId: String) = "${managePhases(curriculumId)}/phases/$phaseId"
    fun editPhase(curriculumId: String, phaseId: String) = "${managedPhase(curriculumId, phaseId)}/edit"
    fun newPhase(curriculumId: String) = "${managePhases(curriculumId)}/new-phase"
}

private val curriculumArguments = listOf(navArgument(Routes.CURRICULUM_ID) { type = NavType.StringType })
private val managedPhaseArguments = curriculumArguments +
    navArgument(Routes.PHASE_ID) { type = NavType.StringType }

private fun NavBackStackEntry.curriculumId() = checkNotNull(arguments?.getString(Routes.CURRICULUM_ID))
private fun NavBackStackEntry.phaseId() = checkNotNull(arguments?.getString(Routes.PHASE_ID))

/** The navigation graph; each destination gets its ViewModel from [container]. */
@Composable
fun AppNavHost(container: AppContainer) {
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
                onManageCurricula = { navController.navigate(Routes.MANAGE_CURRICULA) },
                onShowPhases = { navController.navigate(Routes.PHASES) },
            )
        }
        composable(Routes.MANAGE_CURRICULA) {
            val manage: ManageCurriculaViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ManageCurriculaViewModel(container.curriculumRepository, container.progressRepository) }
                },
            )
            val rows by manage.rows.collectAsStateWithLifecycle()
            ManageCurriculaScreen(
                rows = rows,
                onCreate = manage::create,
                onRename = manage::rename,
                onDelete = manage::delete,
                onCurriculumClick = { navController.navigate(Routes.managePhases(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MANAGE_PHASES, arguments = curriculumArguments) { entry ->
            val curriculumId = entry.curriculumId()
            val manage: ManagePhasesViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ManagePhasesViewModel(
                            curriculumId,
                            container.curriculumRepository,
                            container.progressRepository,
                            container.phaseRepository,
                        )
                    }
                },
            )
            val state by manage.uiState.collectAsStateWithLifecycle()
            ManagePhasesScreen(
                state = state,
                onAdd = { navController.navigate(Routes.newPhase(curriculumId)) },
                onPhaseClick = { navController.navigate(Routes.managedPhase(curriculumId, it)) },
                onDelete = manage::delete,
                onMove = manage::move,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MANAGED_PHASE, arguments = managedPhaseArguments) { entry ->
            val curriculumId = entry.curriculumId()
            val phaseId = entry.phaseId()
            val phase: ManagedPhaseViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ManagedPhaseViewModel(curriculumId, phaseId, container.progressRepository) }
                },
            )
            val state by phase.uiState.collectAsStateWithLifecycle()
            PhaseViewScreen(
                state = state,
                onStart = {},
                onComplete = {},
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editPhase(curriculumId, phaseId)) },
            )
        }
        composable(Routes.EDIT_PHASE, arguments = managedPhaseArguments) { entry ->
            PhaseEditorRoute(container, entry.curriculumId(), entry.phaseId(), onClose = { navController.popBackStack() })
        }
        composable(Routes.NEW_PHASE, arguments = curriculumArguments) { entry ->
            PhaseEditorRoute(container, entry.curriculumId(), phaseId = null, onClose = { navController.popBackStack() })
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
            val phaseId = entry.phaseId()
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
