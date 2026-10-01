package com.bangkoktransit.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.bangkoktransit.app.ui.screens.MapScreen
import com.bangkoktransit.app.ui.screens.PlannerScreen
import com.bangkoktransit.app.ui.screens.SavedTripsScreen
import com.bangkoktransit.app.ui.screens.SettingsScreen
import com.bangkoktransit.app.ui.screens.StationDetailScreen
import com.bangkoktransit.app.ui.screens.StationSearchScreen
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
fun TransitGoApp(
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
) {
    val viewModel: TransitViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var screen: AppScreen by remember { mutableStateOf(AppScreen.Planner) }
    var isMapSheetExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = screen != AppScreen.Planner) {
        val currentScreen = screen
        screen = when (currentScreen) {
            is AppScreen.StationSearch -> if (currentScreen.returnToMap) AppScreen.Map else AppScreen.Planner
            is AppScreen.StationDetail -> AppScreen.Planner
            else -> AppScreen.Planner
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    AppNavigationRail(
                        currentScreen = screen,
                        onSelect = { selectedScreen -> screen = selectedScreen },
                    )
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useNavigationRail) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            BottomNavigationBar(
                                currentScreen = screen,
                                onSelect = { selectedScreen -> screen = selectedScreen },
                            )
                        }
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
                    sheetExpanded = isMapSheetExpanded,
                    onSheetExpandedChange = { isMapSheetExpanded = it },
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
                AppScreen.Settings -> SettingsScreen(
                    darkModeEnabled = darkModeEnabled,
                    onDarkModeChanged = onDarkModeChanged,
                )
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
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentScreen: AppScreen,
    onSelect: (AppScreen) -> Unit,
) {
    val currentTab = currentTabForScreen(currentScreen)

    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        MainTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = { onSelect(tab.screen) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private fun currentTabForScreen(currentScreen: AppScreen): MainTab? = when (currentScreen) {
    AppScreen.Planner -> MainTab.Planner
    AppScreen.Map -> MainTab.Map
    AppScreen.SavedTrips -> MainTab.Saved
    AppScreen.Settings -> MainTab.Settings
    is AppScreen.StationSearch -> if (currentScreen.returnToMap) MainTab.Map else MainTab.Planner
    is AppScreen.StationDetail -> null
}
