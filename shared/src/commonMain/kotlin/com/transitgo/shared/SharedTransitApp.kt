package com.transitgo.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.ui.StationPickerMode
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.ui.screens.MapScreen
import com.bangkoktransit.app.ui.screens.PlannerScreen
import com.bangkoktransit.app.ui.screens.SavedTripsScreen
import com.bangkoktransit.app.ui.screens.StationSearchScreen
import com.bangkoktransit.app.ui.screens.StationDetailScreen
import com.bangkoktransit.app.ui.theme.BangkoktransitTheme
import com.bangkoktransit.app.ui.viewmodel.TransitController
import com.transitgo.shared.ui.screens.SettingsScreen

private enum class SharedTab(val label: String, val icon: ImageVector) {
    Plan("Plan", Icons.Filled.Route),
    Map("Map", Icons.Filled.Map),
    Trips("Trips", Icons.Filled.Bookmark),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun SharedTransitApp(
    versionName: String,
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    controller: TransitController,
    nearbyContent: (@Composable (List<Station>, (Station) -> Unit, () -> Unit) -> Unit)? = null,
) {
    val state by controller.uiState.collectAsState()
    var tabName by rememberSaveable { mutableStateOf(SharedTab.Plan.name) }
    var pickerName by rememberSaveable { mutableStateOf<String?>(null) }
    var detailStationCode by rememberSaveable { mutableStateOf<String?>(null) }
    var mapSheetExpanded by rememberSaveable { mutableStateOf(false) }
    val tab = SharedTab.valueOf(tabName)
    val selectTab: (SharedTab) -> Unit = {
        tabName = it.name
        pickerName = null
        detailStationCode = null
    }

    BangkoktransitTheme(darkTheme = darkModeEnabled) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        SharedTab.entries.forEach { item ->
                            NavigationRailItem(
                                selected = tab == item,
                                onClick = { selectTab(item) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.navigationBars,
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                            ) {
                                SharedTab.entries.forEach { item ->
                                    NavigationBarItem(
                                        selected = tab == item,
                                        onClick = { selectTab(item) },
                                        icon = { Icon(item.icon, contentDescription = null) },
                                        label = { Text(item.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val picker = pickerName?.let(StationPickerMode::valueOf)
                        val selectStation: (Station) -> Unit = { station ->
                            when (picker) {
                                StationPickerMode.Start -> controller.selectStart(station)
                                StationPickerMode.Target -> controller.selectTarget(station)
                                StationPickerMode.Browse -> detailStationCode = station.stationCode
                                null -> Unit
                            }
                            pickerName = null
                        }
                        if (detailStationCode != null) {
                            val station = state.stations.firstOrNull { it.stationCode == detailStationCode }
                            StationDetailScreen(
                                state = state,
                                station = station,
                                onBack = { detailStationCode = null },
                                onSetStart = {
                                    station?.let(controller::selectStart)
                                    selectTab(SharedTab.Plan)
                                },
                                onSetTarget = {
                                    station?.let(controller::selectTarget)
                                    selectTab(SharedTab.Plan)
                                },
                                onShowMap = {
                                    station?.let(controller::selectTarget)
                                    selectTab(SharedTab.Map)
                                },
                            )
                        } else if (picker != null) {
                            key(picker) {
                                StationSearchScreen(
                                    state = state,
                                    mode = picker,
                                    onBack = { pickerName = null },
                                    onStationSelected = selectStation,
                                    onRetry = { controller.refreshTransitData(forceRefresh = true) },
                                    nearbyContent = nearbyContent?.let { content ->
                                        {
                                            content(state.stations, selectStation) {
                                                controller.refreshTransitData(forceRefresh = true)
                                            }
                                        }
                                    },
                                )
                            }
                        } else {
                            when (tab) {
                                SharedTab.Plan -> PlannerScreen(
                                    state = state,
                                    onChooseStart = { pickerName = StationPickerMode.Start.name },
                                    onChooseTarget = { pickerName = StationPickerMode.Target.name },
                                    onSwapStations = controller::swapStations,
                                    onPlanRoute = controller::planRoute,
                                    onSelectRoute = controller::selectRoute,
                                    onToggleSaved = controller::toggleSavedActiveRoute,
                                    onOpenMap = { selectTab(SharedTab.Map) },
                                    onClearRoute = controller::clearRoute,
                                    onRefreshStations = { controller.refreshTransitData(forceRefresh = true) },
                                )
                                SharedTab.Map -> MapScreen(
                                    state = state,
                                    onOpenPlanner = { selectTab(SharedTab.Plan) },
                                    onChooseStart = { pickerName = StationPickerMode.Start.name },
                                    onChooseTarget = { pickerName = StationPickerMode.Target.name },
                                    onSetStart = controller::selectStart,
                                    onSetTarget = controller::selectTarget,
                                    onPlanRoute = controller::planRoute,
                                    onSelectRoute = controller::selectRoute,
                                    onClearRoute = controller::clearRoute,
                                    sheetExpanded = mapSheetExpanded,
                                    onSheetExpandedChange = { mapSheetExpanded = it },
                                    onStationDetails = { detailStationCode = it.stationCode },
                                )
                                SharedTab.Trips -> SavedTripsScreen(
                                    state = state,
                                    onRestoreTrip = {
                                        controller.restoreTrip(it)
                                        selectTab(SharedTab.Plan)
                                    },
                                    onOpenPlanner = { selectTab(SharedTab.Plan) },
                                    onRemoveSavedTrip = controller::removeSavedTrip,
                                    onRemoveRecentTrip = controller::removeRecentTrip,
                                    onClearSavedTrips = controller::clearSavedTrips,
                                    onClearRecentTrips = controller::clearRecentTrips,
                                )
                                SharedTab.Settings -> SettingsScreen(
                                    versionName = versionName,
                                    darkModeEnabled = darkModeEnabled,
                                    onDarkModeChanged = onDarkModeChanged,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
