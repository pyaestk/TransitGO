package com.bangkoktransit.app.data.repository

import android.content.Context
import com.bangkoktransit.app.data.api.BangkokRailwayApi
import com.bangkoktransit.app.data.local.TransitDataCache
import com.bangkoktransit.app.data.model.PlaceGroup
import com.bangkoktransit.app.data.model.RoutePath
import com.bangkoktransit.app.data.model.Station
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TransitRepository(
    context: Context? = null,
    private val api: BangkokRailwayApi = BangkokRailwayApi(),
) {
    private val dataCache = context?.applicationContext?.let(::TransitDataCache)
    private var stationCache: List<Station>? = null
    private var placeCache: List<PlaceGroup>? = null
    private val routeCache = mutableMapOf<String, RouteCacheEntry>()

    suspend fun getStations(forceRefresh: Boolean = false): List<Station> {
        val cached = stationCache
        if (!forceRefresh && cached != null) return cached

        if (!forceRefresh) {
            dataCache?.readStations(STATION_CACHE_MAX_AGE_MS)?.let { json ->
                return api.parseStations(json).also { stationCache = it }
            }

            dataCache?.readFallbackStations()?.let { json ->
                return api.parseStations(json).also { stationCache = it }
            }
        }

        return refreshStations()
    }

    suspend fun refreshStations(): List<Station> {
        val json = api.fetchStationsJson()
        dataCache?.writeStations(json)
        return api.parseStations(json).also { stationCache = it }
    }

    suspend fun getPlaces(forceRefresh: Boolean = false): List<PlaceGroup> {
        val cached = placeCache
        if (!forceRefresh && cached != null) return cached
        return api.fetchPlaces().also { placeCache = it }
    }

    suspend fun getRouteOptions(fromStationCode: String, toStationCode: String): List<RoutePath> {
        val cacheKey = routeCacheKey(fromStationCode, toStationCode)
        val cached = routeCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.savedAt <= ROUTE_CACHE_MAX_AGE_MS) {
            return cached.routes
        }

        val routes = fetchRouteOptions(fromStationCode, toStationCode)
        if (routes.isNotEmpty()) {
            routeCache[cacheKey] = RouteCacheEntry(routes, System.currentTimeMillis())
        }
        return routes
    }

    private suspend fun fetchRouteOptions(
        fromStationCode: String,
        toStationCode: String,
    ): List<RoutePath> = coroutineScope {
        val allPaths = runCatching {
            api.fetchAllPaths(fromStationCode, toStationCode, numPaths = 4)
        }.getOrDefault(emptyList())

        val fallbackPaths = if (allPaths.isEmpty()) {
            val cheapestPath = async {
                runCatching { api.fetchCheapestPath(fromStationCode, toStationCode) }.getOrNull()
            }
            val shortestPath = async {
                runCatching { api.fetchShortestPath(fromStationCode, toStationCode) }.getOrNull()
            }
            listOfNotNull(cheapestPath.await(), shortestPath.await())
        } else {
            allPaths
        }

        fallbackPaths.distinctBy { it.routeKey }
    }

    private fun routeCacheKey(fromStationCode: String, toStationCode: String): String {
        return "${fromStationCode.trim().uppercase()}:${toStationCode.trim().uppercase()}"
    }

    private data class RouteCacheEntry(
        val routes: List<RoutePath>,
        val savedAt: Long,
    )

    private companion object {
        const val STATION_CACHE_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
        const val ROUTE_CACHE_MAX_AGE_MS = 5 * 60 * 1000L
    }
}
