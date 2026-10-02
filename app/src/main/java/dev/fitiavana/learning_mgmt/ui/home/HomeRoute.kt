package dev.fitiavana.learning_mgmt.ui.home

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fitiavana.learning_mgmt.ui.home.drawer.AppDrawerContent
import dev.fitiavana.learning_mgmt.ui.home.drawer.DrawerViewModel
import kotlinx.coroutines.launch

/** Home screen inside the curriculum drawer, wired to its ViewModels. */
@Composable
fun HomeRoute(
    home: HomeViewModel,
    drawer: DrawerViewModel,
    onManageCurricula: () -> Unit,
) {
    val state by home.uiState.collectAsStateWithLifecycle()
    val items by drawer.items.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                items = items,
                onSelect = { id ->
                    drawer.select(id)
                    scope.launch { drawerState.close() }
                },
                onManageCurricula = {
                    scope.launch { drawerState.close() }
                    onManageCurricula()
                },
            )
        },
    ) {
        HomeScreen(
            state = state,
            onStart = home::startNext,
            onComplete = home::completeCurrent,
            onManageCurricula = onManageCurricula,
            onOpenMenu = { scope.launch { drawerState.open() } },
        )
    }
}
