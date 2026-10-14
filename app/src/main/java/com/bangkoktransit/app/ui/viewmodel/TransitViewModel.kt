package com.bangkoktransit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.RoutePreference
import com.bangkoktransit.app.domain.model.SavedTrip
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.domain.usecase.*

class TransitViewModel(
    getStations: GetStations,
    refreshStations: RefreshStations,
    getPlaces: GetPlaces,
    getRouteOptions: GetRouteOptions,
    savedTrips: ManageSavedTrips,
) : ViewModel() {
    private val controller = TransitController(
        viewModelScope, getStations, refreshStations, getPlaces, getRouteOptions, savedTrips,
    )
    val uiState get() = controller.uiState
    fun refreshTransitData(forceRefresh: Boolean = false) = controller.refreshTransitData(forceRefresh)
    fun selectStart(station: Station) = controller.selectStart(station)
    fun selectTarget(station: Station) = controller.selectTarget(station)
    fun swapStations() = controller.swapStations()
    fun clearRoute() = controller.clearRoute()
    fun setRoutePreference(preference: RoutePreference) = controller.setRoutePreference(preference)
    fun selectRoute(route: RoutePath) = controller.selectRoute(route)
    fun planRoute() = controller.planRoute()
    fun toggleSavedActiveRoute() = controller.toggleSavedActiveRoute()
    fun removeSavedTrip(trip: SavedTrip) = controller.removeSavedTrip(trip)
    fun removeRecentTrip(trip: SavedTrip) = controller.removeRecentTrip(trip)
    fun clearSavedTrips() = controller.clearSavedTrips()
    fun clearRecentTrips() = controller.clearRecentTrips()
    fun restoreTrip(trip: SavedTrip) = controller.restoreTrip(trip)
    fun stationByCode(stationCode: String) = controller.stationByCode(stationCode)
}
