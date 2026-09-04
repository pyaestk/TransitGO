package com.bangkoktransit.app.data.local

import android.content.Context
import com.bangkoktransit.app.data.model.RoutePath
import com.bangkoktransit.app.data.model.SavedTrip
import com.bangkoktransit.app.data.model.Station
import org.json.JSONArray
import org.json.JSONObject

class TripStore(context: Context) {
    private val preferences = context.getSharedPreferences("transit_trips", Context.MODE_PRIVATE)

    fun recentTrips(): List<SavedTrip> = readTrips(KEY_RECENT)

    fun savedTrips(): List<SavedTrip> = readTrips(KEY_SAVED)

    fun addRecent(from: Station, to: Station, route: RoutePath) {
        val trip = route.toTrip(from, to)
        val nextTrips = listOf(trip) + recentTrips().filterNot { it.key == trip.key }
        writeTrips(KEY_RECENT, nextTrips.take(MAX_RECENT))
    }

    fun removeRecent(trip: SavedTrip) {
        removeTrip(KEY_RECENT, trip)
    }

    fun removeSaved(trip: SavedTrip) {
        removeTrip(KEY_SAVED, trip)
    }

    fun clearRecent() {
        writeTrips(KEY_RECENT, emptyList())
    }

    fun clearSaved() {
        writeTrips(KEY_SAVED, emptyList())
    }

    fun toggleSaved(from: Station, to: Station, route: RoutePath): Boolean {
        val trip = route.toTrip(from, to)
        val current = savedTrips()
        val exists = current.any { it.key == trip.key }
        val nextTrips = if (exists) {
            current.filterNot { it.key == trip.key }
        } else {
            listOf(trip) + current
        }
        writeTrips(KEY_SAVED, nextTrips)
        return !exists
    }

    fun isSaved(from: Station?, to: Station?, route: RoutePath?): Boolean {
        if (from == null || to == null || route == null) return false
        val key = route.toTrip(from, to).key
        return savedTrips().any { it.key == key }
    }

    private fun RoutePath.toTrip(from: Station, to: Station): SavedTrip {
        return SavedTrip(
            fromCode = from.stationCode,
            fromName = from.nameEn,
            toCode = to.stationCode,
            toName = to.nameEn,
            routeType = pathType,
            fareTotal = fareTotal,
            totalStations = stats.totalStations,
            totalTransfers = stats.totalTransfers,
            savedAt = System.currentTimeMillis(),
        )
    }

    private fun readTrips(key: String): List<SavedTrip> {
        val raw = preferences.getString(key, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        SavedTrip(
                            fromCode = item.optString("fromCode"),
                            fromName = item.optString("fromName"),
                            toCode = item.optString("toCode"),
                            toName = item.optString("toName"),
                            routeType = item.optString("routeType"),
                            fareTotal = item.optDoubleOrNull("fareTotal"),
                            totalStations = item.optInt("totalStations"),
                            totalTransfers = item.optInt("totalTransfers"),
                            savedAt = item.optLong("savedAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeTrips(key: String, trips: List<SavedTrip>) {
        val array = JSONArray()
        trips.forEach { trip ->
            array.put(
                JSONObject()
                    .put("fromCode", trip.fromCode)
                    .put("fromName", trip.fromName)
                    .put("toCode", trip.toCode)
                    .put("toName", trip.toName)
                    .put("routeType", trip.routeType)
                    .put("fareTotal", trip.fareTotal)
                    .put("totalStations", trip.totalStations)
                    .put("totalTransfers", trip.totalTransfers)
                    .put("savedAt", trip.savedAt),
            )
        }
        preferences.edit().putString(key, array.toString()).apply()
    }

    private fun removeTrip(key: String, trip: SavedTrip) {
        val nextTrips = readTrips(key).filterNot { it.key == trip.key }
        writeTrips(key, nextTrips)
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optDouble(name)
    }

    private companion object {
        const val KEY_RECENT = "recent"
        const val KEY_SAVED = "saved"
        const val MAX_RECENT = 8
    }
}
