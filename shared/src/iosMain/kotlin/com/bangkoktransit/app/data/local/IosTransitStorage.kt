package com.bangkoktransit.app.data.local

import kotlin.time.Clock
import platform.Foundation.NSUserDefaults

class IosStringPreferences(private val name: String) : StringPreferences {
    private val preferences = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = preferences.stringForKey("$name.$key")

    override fun setString(key: String, value: String) {
        preferences.setObject(value, forKey = "$name.$key")
    }
}

class IosTransitDataCache : StationDataCache {
    private val preferences = IosStringPreferences("transit_data")

    override fun readStations(maxAgeMillis: Long): String? {
        val savedAt = preferences.getString("stations_saved_at")?.toLongOrNull() ?: return null
        val age = Clock.System.now().toEpochMilliseconds() - savedAt
        if (age !in 0..maxAgeMillis) return null
        return preferences.getString("stations_json")?.takeIf { it.isNotBlank() }
    }

    override fun writeStations(json: String) {
        if (json.isBlank()) return
        preferences.setString("stations_json", json)
        preferences.setString("stations_saved_at", Clock.System.now().toEpochMilliseconds().toString())
    }

    override suspend fun readFallbackStations(): String = readBundledStations()
}
