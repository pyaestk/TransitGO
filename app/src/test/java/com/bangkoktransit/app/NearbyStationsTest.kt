package com.bangkoktransit.app

import com.bangkoktransit.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class NearbyStationsTest {
    private fun station(code: String, lat: Double?, lon: Double?) =
        Station(1, code, code, code, latitude = lat, longitude = lon)

    @Test fun selectsOnlyNearbyStationsInDistanceOrder() {
        val result = nearbyStations(listOf(
            station("far", 14.0, 100.5), station("near", 13.7505, 100.5),
            station("here", 13.75, 100.5), station("missing", null, null),
            station("invalid", Double.NaN, 100.5), station("partial", 13.75, null),
        ), 13.75, 100.5)
        assertEquals(listOf("here", "near"), result.map { it.station.stationCode })
        assertEquals(55.6, result[1].distanceMeters, 0.2)
    }

    @Test fun doesNotFallBackToDistantOrMapCoordinates() {
        assertTrue(nearbyStations(listOf(station("far", 13.75, 100.5)), 18.0, 99.0).isEmpty())
        assertTrue(nearbyStations(listOf(Station(1, "map", "map", "map", x = 13.75, y = 100.5)), 13.75, 100.5).isEmpty())
    }

    @Test fun radiusAndLimitAreEnforced() {
        val stations = (1..8).map { station("$it", 13.75 + it * 0.001, 100.5) }
        assertEquals(5, nearbyStations(stations, 13.75, 100.5).size)
        val boundary = listOf(station("inside", 13.75 + 0.0179, 100.5), station("outside", 13.75 + 0.0181, 100.5))
        assertEquals(listOf("inside"), nearbyStations(boundary, 13.75, 100.5).map { it.station.stationCode })
    }

    @Test fun invalidUserLocationProducesNoSuggestions() {
        assertTrue(nearbyStations(listOf(station("a", 13.75, 100.5)), Double.NaN, 100.5).isEmpty())
    }
}
