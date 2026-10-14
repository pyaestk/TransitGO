package com.bangkoktransit.app.domain.repository

import com.bangkoktransit.app.domain.model.PlaceGroup
import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.Station

interface TransitRepository {
    suspend fun getStations(forceRefresh: Boolean = false): List<Station>
    suspend fun refreshStations(): List<Station>
    suspend fun getPlaces(forceRefresh: Boolean = false): List<PlaceGroup>
    suspend fun getRouteOptions(fromStationCode: String, toStationCode: String): List<RoutePath>
}
