package com.bangkoktransit.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bangkoktransit.app.data.local.TripStore
import com.bangkoktransit.app.data.model.PlaceGroup
import com.bangkoktransit.app.data.model.RoutePath
import com.bangkoktransit.app.data.model.RoutePreference
import com.bangkoktransit.app.data.model.SavedTrip
import com.bangkoktransit.app.data.model.Station
import com.bangkoktransit.app.data.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransitUiState(
    val stations: List<Station> = emptyList(),
    val places: List<PlaceGroup> = emptyList(),
    val isLoadingStations: Boolean = false,
    val stationError: String? = null,
    val selectedStart: Station? = null,
    val selectedTarget: Station? = null,
    val routePreference: RoutePreference = RoutePreference.Recommended,
    val routeOptions: List<RoutePath> = emptyList(),
    val activeRoute: RoutePath? = null,
    val isPlanningRoute: Boolean = false,
    val routeError: String? = null,
    val savedTrips: List<SavedTrip> = emptyList(),
    val recentTrips: List<SavedTrip> = emptyList(),
    val isActiveRouteSaved: Boolean = false,
)

class TransitViewModel(
    application: Application,
    private val repository: TransitRepository = TransitRepository(application),
    private val tripStore: TripStore = TripStore(application),
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        TransitUiState(
            savedTrips = tripStore.savedTrips(),
            recentTrips = tripStore.recentTrips(),
        ),
    )
    val uiState: StateFlow<TransitUiState> = _uiState

    init {
        refreshTransitData()
    }

    fun refreshTransitData(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoadingStations) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingStations = true,
                    stationError = null,
                )
            }

            try {
                val stations = repository.getStations(forceRefresh)
                _uiState.update {
                    it.copy(
                        stations = stations,
                        isLoadingStations = false,
                        stationError = null,
                    )
                }

                val places = runCatching { repository.getPlaces(forceRefresh) }
                    .getOrDefault(emptyList())

                _uiState.update {
                    it.copy(places = places)
                }

                if (!forceRefresh) {
                    runCatching { repository.refreshStations() }
                        .onSuccess { freshStations ->
                            _uiState.update {
                                it.copy(
                                    stations = freshStations,
                                    stationError = null,
                                )
                            }
                        }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingStations = false,
                        stationError = error.message ?: "Unable to load station data.",
                    )
                }
            }
        }
    }

    fun selectStart(station: Station) {
        _uiState.update {
            it.copy(
                selectedStart = station,
                routeOptions = emptyList(),
                activeRoute = null,
                routeError = null,
                isActiveRouteSaved = false,
            )
        }
    }

    fun selectTarget(station: Station) {
        _uiState.update {
            it.copy(
                selectedTarget = station,
                routeOptions = emptyList(),
                activeRoute = null,
                routeError = null,
                isActiveRouteSaved = false,
            )
        }
    }

    fun swapStations() {
        _uiState.update {
            it.copy(
                selectedStart = it.selectedTarget,
                selectedTarget = it.selectedStart,
                routeOptions = emptyList(),
                activeRoute = null,
                routeError = null,
                isActiveRouteSaved = false,
            )
        }
    }

    fun clearRoute() {
        _uiState.update {
            it.copy(
                selectedStart = null,
                selectedTarget = null,
                routeOptions = emptyList(),
                activeRoute = null,
                isPlanningRoute = false,
                routeError = null,
                isActiveRouteSaved = false,
            )
        }
    }

    fun setRoutePreference(preference: RoutePreference) {
        _uiState.update { state ->
            val selected = chooseRoute(state.routeOptions, preference)
            state.copy(
                routePreference = preference,
                activeRoute = selected,
                isActiveRouteSaved = tripStore.isSaved(
                    state.selectedStart,
                    state.selectedTarget,
                    selected,
                ),
            )
        }
    }

    fun selectRoute(route: RoutePath) {
        _uiState.update {
            it.copy(
                activeRoute = route,
                isActiveRouteSaved = tripStore.isSaved(it.selectedStart, it.selectedTarget, route),
            )
        }
    }

    fun planRoute() {
        val start = _uiState.value.selectedStart
        val target = _uiState.value.selectedTarget

        if (start == null || target == null) {
            _uiState.update { it.copy(routeError = "Choose both start and destination stations.") }
            return
        }

        if (start.stationCode == target.stationCode) {
            _uiState.update { it.copy(routeError = "Start and destination must be different.") }
            return
        }

        if (_uiState.value.isPlanningRoute) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPlanningRoute = true,
                    routeError = null,
                    routeOptions = emptyList(),
                    activeRoute = null,
                    isActiveRouteSaved = false,
                )
            }

            try {
                val routes = repository.getRouteOptions(start.stationCode, target.stationCode)
                if (routes.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isPlanningRoute = false,
                            routeError = "No route options found for these stations.",
                        )
                    }
                    return@launch
                }

                val selectedRoute = chooseRoute(routes, _uiState.value.routePreference) ?: routes.first()
                tripStore.addRecent(start, target, selectedRoute)

                _uiState.update {
                    it.copy(
                        routeOptions = routes,
                        activeRoute = selectedRoute,
                        isPlanningRoute = false,
                        routeError = null,
                        recentTrips = tripStore.recentTrips(),
                        savedTrips = tripStore.savedTrips(),
                        isActiveRouteSaved = tripStore.isSaved(start, target, selectedRoute),
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isPlanningRoute = false,
                        routeError = error.message ?: "Unable to plan this route.",
                    )
                }
            }
        }
    }

    fun toggleSavedActiveRoute() {
        val state = _uiState.value
        val start = state.selectedStart ?: return
        val target = state.selectedTarget ?: return
        val route = state.activeRoute ?: return
        val isSavedNow = tripStore.toggleSaved(start, target, route)
        _uiState.update {
            it.copy(
                savedTrips = tripStore.savedTrips(),
                recentTrips = tripStore.recentTrips(),
                isActiveRouteSaved = isSavedNow,
            )
        }
    }

    fun removeSavedTrip(trip: SavedTrip) {
        tripStore.removeSaved(trip)
        refreshStoredTrips()
    }

    fun removeRecentTrip(trip: SavedTrip) {
        tripStore.removeRecent(trip)
        refreshStoredTrips()
    }

    fun clearSavedTrips() {
        tripStore.clearSaved()
        refreshStoredTrips()
    }

    fun clearRecentTrips() {
        tripStore.clearRecent()
        refreshStoredTrips()
    }

    fun restoreTrip(trip: SavedTrip) {
        val state = _uiState.value
        val start = state.stations.find { it.stationCode == trip.fromCode }
            ?: trip.toPlaceholderStation(isStart = true)
        val target = state.stations.find { it.stationCode == trip.toCode }
            ?: trip.toPlaceholderStation(isStart = false)

        _uiState.update {
            it.copy(
                selectedStart = start,
                selectedTarget = target,
                routeOptions = emptyList(),
                activeRoute = null,
                routeError = null,
                isActiveRouteSaved = false,
            )
        }
        planRoute()
    }

    fun stationByCode(stationCode: String): Station? {
        return _uiState.value.stations.find { it.stationCode == stationCode }
    }

    private fun refreshStoredTrips() {
        _uiState.update {
            it.copy(
                savedTrips = tripStore.savedTrips(),
                recentTrips = tripStore.recentTrips(),
                isActiveRouteSaved = tripStore.isSaved(
                    it.selectedStart,
                    it.selectedTarget,
                    it.activeRoute,
                ),
            )
        }
    }

    private fun chooseRoute(
        routes: List<RoutePath>,
        preference: RoutePreference,
    ): RoutePath? {
        return when (preference) {
            RoutePreference.Recommended -> {
                routes.firstOrNull { it.pathType.equals("cheapest", ignoreCase = true) }
                    ?: routes.minWithOrNull(
                        compareBy<RoutePath> { it.fareTotal ?: Double.MAX_VALUE }
                            .thenBy { it.stats.totalTransfers }
                            .thenBy { it.stats.totalStations },
                    )
            }
            RoutePreference.Cheapest -> routes.minByOrNull { it.fareTotal ?: Double.MAX_VALUE }
            RoutePreference.FewestStations -> routes.minByOrNull { it.stats.totalStations }
            RoutePreference.FewestTransfers -> routes.minWithOrNull(
                compareBy<RoutePath> { it.stats.totalTransfers }
                    .thenBy { it.stats.totalStations },
            )
        }
    }

    private fun SavedTrip.toPlaceholderStation(isStart: Boolean): Station {
        return Station(
            id = 0,
            stationCode = if (isStart) fromCode else toCode,
            stationShortName = if (isStart) fromCode else toCode,
            nameEn = if (isStart) fromName else toName,
        )
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TransitViewModel(application) as T
                }
            }
        }
    }
}
