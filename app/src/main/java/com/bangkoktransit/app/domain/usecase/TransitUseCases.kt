package com.bangkoktransit.app.domain.usecase

import com.bangkoktransit.app.domain.model.PlaceGroup
import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.SavedTrip
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.domain.repository.SavedTripsRepository
import com.bangkoktransit.app.domain.repository.TransitRepository

class GetStations(private val repository: TransitRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<Station> =
        repository.getStations(forceRefresh)
}

class RefreshStations(private val repository: TransitRepository) {
    suspend operator fun invoke(): List<Station> = repository.refreshStations()
}

class GetPlaces(private val repository: TransitRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<PlaceGroup> =
        repository.getPlaces(forceRefresh)
}

class GetRouteOptions(private val repository: TransitRepository) {
    suspend operator fun invoke(fromStationCode: String, toStationCode: String): List<RoutePath> =
        repository.getRouteOptions(fromStationCode, toStationCode)
}

class ManageSavedTrips(private val repository: SavedTripsRepository) {
    fun recentTrips() = repository.recentTrips()
    fun savedTrips() = repository.savedTrips()
    fun addRecent(from: Station, to: Station, route: RoutePath) = repository.addRecent(from, to, route)
    fun removeRecent(trip: SavedTrip) = repository.removeRecent(trip)
    fun removeSaved(trip: SavedTrip) = repository.removeSaved(trip)
    fun clearRecent() = repository.clearRecent()
    fun clearSaved() = repository.clearSaved()
    fun toggleSaved(from: Station, to: Station, route: RoutePath) =
        repository.toggleSaved(from, to, route)
    fun isSaved(from: Station?, to: Station?, route: RoutePath?) =
        repository.isSaved(from, to, route)
}
