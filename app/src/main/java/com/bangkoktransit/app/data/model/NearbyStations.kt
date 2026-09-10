package com.bangkoktransit.app.data.model

import kotlin.math.*

const val NEARBY_RADIUS_METERS = 2000.0

data class NearbyStation(val station: Station, val distanceMeters: Double)

fun Station.hasGpsCoordinates(): Boolean =
    latitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
        longitude?.let { it.isFinite() && it in -180.0..180.0 } == true

/** Great-circle distance; this does not estimate walking distance. */
fun nearbyStations(stations: List<Station>, latitude: Double, longitude: Double): List<NearbyStation> {
    if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0) return emptyList()
    return stations.filter { it.hasGpsCoordinates() }.map { station ->
        val latDelta = Math.toRadians(station.latitude!! - latitude)
        val lonDelta = Math.toRadians(station.longitude!! - longitude)
        val a = sin(latDelta / 2).pow(2) + cos(Math.toRadians(latitude)) *
            cos(Math.toRadians(station.latitude)) * sin(lonDelta / 2).pow(2)
        NearbyStation(station, 6371000.0 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0))))
    }.filter { it.distanceMeters <= NEARBY_RADIUS_METERS }
        .sortedWith(compareBy<NearbyStation> { it.distanceMeters }.thenBy { it.station.stationCode })
        .take(5)
}
