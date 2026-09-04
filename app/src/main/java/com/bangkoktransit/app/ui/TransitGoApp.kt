package com.bangkoktransit.app.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bangkoktransit.app.ui.screens.MapScreen
import com.bangkoktransit.app.ui.screens.PlannerScreen
import com.bangkoktransit.app.ui.screens.SavedTripsScreen
import com.bangkoktransit.app.ui.screens.SettingsScreen
import com.bangkoktransit.app.ui.screens.StationDetailScreen
import com.bangkoktransit.app.ui.screens.StationSearchScreen
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitLine
import com.bangkoktransit.app.ui.viewmodel.TransitViewModel

sealed interface AppScreen {
    data object Planner : AppScreen
    data object Map : AppScreen
    data object SavedTrips : AppScreen
    data object Settings : AppScreen
    data class StationSearch(val mode: StationPickerMode, val returnToMap: Boolean = false) : AppScreen
    data class StationDetail(val stationCode: String) : AppScreen
}

enum class StationPickerMode {
    Browse,
    Start,
    Target,
}

private enum class MainTab(val label: String, val icon: ImageVector, val screen: AppScreen) {
    Planner("Plan", Icons.Filled.Route, AppScreen.Planner),
    Map("Map", Icons.Filled.Map, AppScreen.Map),
    Saved("Trips", Icons.Filled.Bookmark, AppScreen.SavedTrips),
    Settings("Settings", Icons.Filled.Settings, AppScreen.Settings),
}

@Composable
fun TransitGoApp() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: TransitViewModel = viewModel(
        factory = TransitViewModel.factory(application),
    )
    val state by viewModel.uiState.collectAsState()
    var screen: AppScreen by remember { mutableStateOf(AppScreen.Planner) }

    BackHandler(enabled = screen != AppScreen.Planner) {
        val currentScreen = screen
        screen = when (currentScreen) {
            is AppScreen.StationSearch -> if (currentScreen.returnToMap) AppScreen.Map else AppScreen.Planner
            is AppScreen.StationDetail -> AppScreen.Planner
            else -> AppScreen.Planner
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, TransitLine),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                BottomNavigationBar(
                    currentScreen = screen,
                    onSelect = { selectedScreen -> screen = selectedScreen },
                )
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            val openPlanner: () -> Unit = { screen = AppScreen.Planner }
            val openMap: () -> Unit = { screen = AppScreen.Map }
            val openStationSearch: (StationPickerMode, Boolean) -> Unit = { mode, returnToMap ->
                screen = AppScreen.StationSearch(mode, returnToMap)
            }

            when (val current = screen) {
                AppScreen.Planner -> PlannerScreen(
                    state = state,
                    onChooseStart = { openStationSearch(StationPickerMode.Start, false) },
                    onChooseTarget = { openStationSearch(StationPickerMode.Target, false) },
                    onSwapStations = viewModel::swapStations,
                    onPlanRoute = viewModel::planRoute,
                    onSelectRoute = viewModel::selectRoute,
                    onToggleSaved = viewModel::toggleSavedActiveRoute,
                    onOpenMap = openMap,
                    onClearRoute = viewModel::clearRoute,
                    onRefreshStations = { viewModel.refreshTransitData(forceRefresh = true) },
                )
                AppScreen.Map -> MapScreen(
                    state = state,
                    onOpenPlanner = openPlanner,
                    onChooseStart = { openStationSearch(StationPickerMode.Start, true) },
                    onChooseTarget = { openStationSearch(StationPickerMode.Target, true) },
                    onSetStart = viewModel::selectStart,
                    onSetTarget = viewModel::selectTarget,
                    onPlanRoute = viewModel::planRoute,
                    onSelectRoute = viewModel::selectRoute,
                    onClearRoute = viewModel::clearRoute,
                )
                AppScreen.SavedTrips -> SavedTripsScreen(
                    state = state,
                    onRestoreTrip = { trip ->
                        viewModel.restoreTrip(trip)
                        screen = AppScreen.Planner
                    },
                    onOpenPlanner = openPlanner,
                    onRemoveSavedTrip = viewModel::removeSavedTrip,
                    onRemoveRecentTrip = viewModel::removeRecentTrip,
                    onClearSavedTrips = viewModel::clearSavedTrips,
                    onClearRecentTrips = viewModel::clearRecentTrips,
                )
                AppScreen.Settings -> SettingsScreen()
                is AppScreen.StationSearch -> StationSearchScreen(
                    state = state,
                    mode = current.mode,
                    onBack = {
                        screen = if (current.returnToMap) AppScreen.Map else AppScreen.Planner
                    },
                    onStationSelected = { station ->
                        when (current.mode) {
                            StationPickerMode.Browse -> {
                                screen = AppScreen.StationDetail(station.stationCode)
                            }
                            StationPickerMode.Start -> {
                                viewModel.selectStart(station)
                                screen = if (current.returnToMap) AppScreen.Map else AppScreen.Planner
                            }
                            StationPickerMode.Target -> {
                                viewModel.selectTarget(station)
                                screen = if (current.returnToMap) AppScreen.Map else AppScreen.Planner
                            }
                        }
                    },
                    onRetry = { viewModel.refreshTransitData(forceRefresh = true) },
                )
                is AppScreen.StationDetail -> {
                    val station = viewModel.stationByCode(current.stationCode)
                    StationDetailScreen(
                        state = state,
                        station = station,
                        onBack = { screen = AppScreen.Planner },
                        onSetStart = {
                            if (station != null) viewModel.selectStart(station)
                            screen = AppScreen.Planner
                        },
                        onSetTarget = {
                            if (station != null) viewModel.selectTarget(station)
                            screen = AppScreen.Planner
                        },
                        onShowMap = {
                            if (station != null) {
                                viewModel.selectTarget(station)
                            }
                            screen = AppScreen.Map
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentScreen: AppScreen,
    onSelect: (AppScreen) -> Unit,
) {
    val currentTab = when (currentScreen) {
        AppScreen.Planner -> MainTab.Planner
        AppScreen.Map -> MainTab.Map
        AppScreen.SavedTrips -> MainTab.Saved
        AppScreen.Settings -> MainTab.Settings
        is AppScreen.StationSearch -> if (currentScreen.returnToMap) MainTab.Map else MainTab.Planner
        is AppScreen.StationDetail -> null
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onSelect(tab.screen) },
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = null)
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TransitBlue,
                    selectedTextColor = TransitBlue,
                    indicatorColor = TransitBlue.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
