package com.bangkoktransit.app

import com.bangkoktransit.app.data.local.StringPreferences
import com.bangkoktransit.app.data.local.TripStore
import com.bangkoktransit.app.domain.model.*
import kotlin.test.*

internal class TestPreferences : StringPreferences {
    val values = mutableMapOf<String, String>()
    override fun getString(key: String): String? = values[key]
    override fun setString(key: String, value: String) { values[key] = value }
}

internal fun testStation(code: String) = Station(0, code, code, code)
internal fun testRoute(fare: Double? = null) = RoutePath(
    "cheapest", "CEN_NE", "E04", PathStats(5, 0, 1), "Siam to Asok", emptyList(), emptyList(), fare,
)

class TripStoreTest {
    @Test fun existingAndroidSavedTripJsonCanBeReadAndRemoved() {
        val preferences = TestPreferences()
        preferences.values["saved"] = """[{"fromCode":"CEN_NE","fromName":"Siam","toCode":"E04","toName":"Asok","routeType":"cheapest","fareTotal":35.5,"totalStations":5,"totalTransfers":0,"savedAt":1234}]"""
        val store = TripStore(preferences)
        val trip = store.savedTrips().single()
        assertEquals(35.5, trip.fareTotal)
        assertEquals(1234L, trip.savedAt)
        assertFalse(store.toggleSaved(testStation("CEN_NE"), testStation("E04"), testRoute(35.5)))
        assertTrue(store.savedTrips().isEmpty())
    }

    @Test fun savedTripsSurviveStoreRecreationWithMissingFare() {
        val preferences = TestPreferences()
        val from = testStation("CEN_NE")
        val to = testStation("E04")
        assertTrue(TripStore(preferences).toggleSaved(from, to, testRoute()))
        val restored = TripStore(preferences)
        assertTrue(restored.isSaved(from, to, testRoute()))
        assertNull(restored.savedTrips().single().fareTotal)
    }

    @Test fun recentTripsStayUniqueAndKeepOnlyEight() {
        val store = TripStore(TestPreferences())
        val from = testStation("CEN_NE")
        for (index in 0..9) store.addRecent(from, testStation("E$index"), testRoute())
        store.addRecent(from, testStation("E9"), testRoute())
        assertEquals(8, store.recentTrips().size)
        assertEquals("E9", store.recentTrips().first().toCode)
        assertEquals(8, store.recentTrips().map { it.key }.distinct().size)
    }
}
