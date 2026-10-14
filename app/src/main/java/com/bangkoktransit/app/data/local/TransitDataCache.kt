package com.bangkoktransit.app.data.local

import android.content.Context
import kotlinx.coroutines.CancellationException

class TransitDataCache(context: Context) : StationDataCache {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("transit_data", Context.MODE_PRIVATE)

    override fun readStations(maxAgeMillis: Long): String? {
        val savedAt = preferences.getLong(KEY_STATIONS_SAVED_AT, 0L)
        val json = preferences.getString(KEY_STATIONS_JSON, null)
        val isFresh = savedAt > 0L && System.currentTimeMillis() - savedAt <= maxAgeMillis
        return json?.takeIf { isFresh && it.isNotBlank() }
    }

    override fun writeStations(json: String) {
        if (json.isBlank()) return
        preferences.edit()
            .putString(KEY_STATIONS_JSON, json)
            .putLong(KEY_STATIONS_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    override suspend fun readFallbackStations(): String? {
        return try {
            readBundledStations()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            null
        }
    }

    private companion object {
        const val KEY_STATIONS_JSON = "stations_json"
        const val KEY_STATIONS_SAVED_AT = "stations_saved_at"
    }
}
