package com.bangkoktransit.app.data.local

interface StationDataCache {
    fun readStations(maxAgeMillis: Long): String?
    fun writeStations(json: String)
    suspend fun readFallbackStations(): String?
}

interface StringPreferences {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
}
