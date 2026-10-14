package com.bangkoktransit.app.domain.repository

import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.SavedTrip
import com.bangkoktransit.app.domain.model.Station

interface SavedTripsRepository {
    fun recentTrips(): List<SavedTrip>
    fun savedTrips(): List<SavedTrip>
    fun addRecent(from: Station, to: Station, route: RoutePath)
    fun removeRecent(trip: SavedTrip)
    fun removeSaved(trip: SavedTrip)
    fun clearRecent()
    fun clearSaved()
    fun toggleSaved(from: Station, to: Station, route: RoutePath): Boolean
    fun isSaved(from: Station?, to: Station?, route: RoutePath?): Boolean
}
