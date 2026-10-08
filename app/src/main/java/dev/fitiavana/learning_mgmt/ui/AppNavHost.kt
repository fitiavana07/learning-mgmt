package dev.fitiavana.learning_mgmt.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
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
import dev.fitiavana.learning_mgmt.ui.managecurricula.ManageCurriculaRoute
import dev.fitiavana.learning_mgmt.ui.managecurricula.ManageCurriculaViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.ManagePhasesScreen
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.ManagePhasesViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor.ManagedPhaseViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.ManageTopicsScreen
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.ManageTopicsViewModel
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.managetopics.topiceditor.TopicEditorRoute
import dev.fitiavana.learning_mgmt.ui.managecurricula.managephases.phaseeditor.PhaseEditorRoute
import dev.fitiavana.learning_mgmt.ui.managecurricula.sync.SyncScreen
import dev.fitiavana.learning_mgmt.ui.managecurricula.sync.SyncViewModel

private object Routes {
    const val HOME = "home"
    const val PHASES = "phases"
    const val PHASE_ID = "phaseId"
    const val PHASE = "phases/{$PHASE_ID}"
    fun phase(id: String) = "phases/$id"

    const val SYNC = "sync"

    const val MANAGE_CURRICULA = "manage-curricula"
    const val CURRICULUM_ID = "curriculumId"
    const val MANAGE_PHASES = "$MANAGE_CURRICULA/{$CURRICULUM_ID}"
    fun managePhases(curriculumId: String) = "$MANAGE_CURRICULA/$curriculumId"

    const val MANAGED_PHASE = "$MANAGE_PHASES/phases/{$PHASE_ID}"
    const val EDIT_PHASE = "$MANAGED_PHASE/edit"
    const val NEW_PHASE = "$MANAGE_PHASES/new-phase"
    const val TOPICS = "$MANAGED_PHASE/topics"
    const val TOPIC_ID = "topicId"
    const val NEW_TOPIC = "$TOPICS/new-topic"
    const val EDIT_TOPIC = "$TOPICS/{$TOPIC_ID}/edit"
    fun managedPhase(curriculumId: String, phaseId: String) = "${managePhases(curriculumId)}/phases/$phaseId"
    fun editPhase(curriculumId: String, phaseId: String) = "${managedPhase(curriculumId, phaseId)}/edit"
    fun newPhase(curriculumId: String) = "${managePhases(curriculumId)}/new-phase"
    fun topics(curriculumId: String, phaseId: String) = "${managedPhase(curriculumId, phaseId)}/topics"
    fun newTopic(curriculumId: String, phaseId: String) = "${topics(curriculumId, phaseId)}/new-topic"
    fun editTopic(curriculumId: String, phaseId: String, topicId: String) =
        "${topics(curriculumId, phaseId)}/$topicId/edit"
}

private val curriculumArguments = listOf(navArgument(Routes.CURRICULUM_ID) { type = NavType.StringType })
private val managedPhaseArguments = curriculumArguments +
    navArgument(Routes.PHASE_ID) { type = NavType.StringType }

private fun NavBackStackEntry.curriculumId() = checkNotNull(arguments?.getString(Routes.CURRICULUM_ID))
private val editTopicArguments = managedPhaseArguments +
    navArgument(Routes.TOPIC_ID) { type = NavType.StringType }

private fun NavBackStackEntry.phaseId() = checkNotNull(arguments?.getString(Routes.PHASE_ID))
private fun NavBackStackEntry.topicId() = checkNotNull(arguments?.getString(Routes.TOPIC_ID))

private const val SLIDE_MS = 300
private const val BEHIND_FRACTION = 4

/** The incoming screen travels the full width; the one it covers only shifts a fraction. */
private fun slideIn(fromEnd: Boolean) = slideInHorizontally(tween(SLIDE_MS)) { if (fromEnd) it else -it / BEHIND_FRACTION }

private fun slideOut(toStart: Boolean) = slideOutHorizontally(tween(SLIDE_MS)) { if (toStart) -it / BEHIND_FRACTION else it }

/**
 * A destination that slides. Only used for the phase screens: Home <-> Manage curricula
 * must stay instant (a crossfade there blanked the window when the drawer opened mid-animation).
 */
private fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) = composable(
    route,
    arguments = arguments,
    enterTransition = { slideIn(fromEnd = true) },
    exitTransition = { slideOut(toStart = true) },
    popEnterTransition = { slideIn(fromEnd = false) },
    popExitTransition = { slideOut(toStart = false) },
    content = { content(it) },
)

/** The navigation graph; each destination gets its ViewModel from [container]. */
@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController,
        startDestination = Routes.HOME,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
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
                    initializer {
                        ManageCurriculaViewModel(
                            container.curriculumRepository,
                            container.progressRepository,
                            container.backupRepository,
                        )
                    }
                },
            )
            ManageCurriculaRoute(
                viewModel = manage,
                onCurriculumClick = { navController.navigate(Routes.managePhases(it)) },
                onSync = { navController.navigate(Routes.SYNC) },
                onBack = { navController.popBackStack() },
            )
        }
        animatedComposable(Routes.SYNC) {
            val sync: SyncViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { SyncViewModel(container.syncCoordinator, container.syncSettings) }
                },
            )
            val state by sync.state.collectAsStateWithLifecycle()
            SyncScreen(
                state = state,
                onSavePassphrase = sync::savePassphrase,
                onRemovePassphrase = sync::removePassphrase,
                onAutoSyncChange = sync::setAutoSync,
                onRefresh = sync::refresh,
                onSyncWith = sync::syncWith,
                onSyncAll = sync::syncAll,
                onBack = { navController.popBackStack() },
            )
        }
        animatedComposable(Routes.MANAGE_PHASES, arguments = curriculumArguments) { entry ->
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
        animatedComposable(Routes.MANAGED_PHASE, arguments = managedPhaseArguments) { entry ->
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
        animatedComposable(Routes.EDIT_PHASE, arguments = managedPhaseArguments) { entry ->
            val curriculumId = entry.curriculumId()
            val phaseId = entry.phaseId()
            PhaseEditorRoute(
                container,
                curriculumId,
                phaseId,
                onClose = { navController.popBackStack() },
                onManageTopics = { navController.navigate(Routes.topics(curriculumId, phaseId)) },
            )
        }
        animatedComposable(Routes.TOPICS, arguments = managedPhaseArguments) { entry ->
            val curriculumId = entry.curriculumId()
            val phaseId = entry.phaseId()
            val manage: ManageTopicsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ManageTopicsViewModel(curriculumId, phaseId, container.progressRepository, container.topicRepository)
                    }
                },
            )
            val state by manage.uiState.collectAsStateWithLifecycle()
            ManageTopicsScreen(
                state = state,
                onAdd = { navController.navigate(Routes.newTopic(curriculumId, phaseId)) },
                onTopicClick = { navController.navigate(Routes.editTopic(curriculumId, phaseId, it)) },
                onDelete = manage::delete,
                onMove = manage::move,
                onBack = { navController.popBackStack() },
            )
        }
        animatedComposable(Routes.NEW_TOPIC, arguments = managedPhaseArguments) { entry ->
            TopicEditorRoute(container, entry.phaseId(), topicId = null, onClose = { navController.popBackStack() })
        }
        animatedComposable(Routes.EDIT_TOPIC, arguments = editTopicArguments) { entry ->
            TopicEditorRoute(container, entry.phaseId(), entry.topicId(), onClose = { navController.popBackStack() })
        }
        animatedComposable(Routes.NEW_PHASE, arguments = curriculumArguments) { entry ->
            PhaseEditorRoute(
                container,
                entry.curriculumId(),
                phaseId = null,
                onClose = { navController.popBackStack() },
                onManageTopics = {},
            )
        }
        animatedComposable(Routes.PHASES) {
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
        animatedComposable(Routes.PHASE, arguments = listOf(navArgument(Routes.PHASE_ID) { type = NavType.StringType })) { entry ->
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
                topicHandlers = phase.topicHandlers,
            )
        }
    }
}
