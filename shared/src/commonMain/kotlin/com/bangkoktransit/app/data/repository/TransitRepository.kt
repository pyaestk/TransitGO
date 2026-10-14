package com.bangkoktransit.app.data.repository

import com.bangkoktransit.app.data.api.BangkokRailwayApi
import com.bangkoktransit.app.data.local.StationDataCache
import com.bangkoktransit.app.util.cancellableRunCatching
import kotlin.time.Clock
import com.bangkoktransit.app.domain.model.PlaceGroup
import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.domain.repository.TransitRepository as TransitRepositoryContract
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TransitRepository(
    private val dataCache: StationDataCache,
    private val api: BangkokRailwayApi = BangkokRailwayApi(),
) : TransitRepositoryContract {
    private var stationCache: List<Station>? = null
    private var placeCache: List<PlaceGroup>? = null
    private val routeCache = mutableMapOf<String, RouteCacheEntry>()

    override suspend fun getStations(forceRefresh: Boolean): List<Station> {
        val cached = stationCache
        if (!forceRefresh && cached != null) return cached

        if (!forceRefresh) {
            dataCache.readStations(STATION_CACHE_MAX_AGE_MS)?.let { json ->
                return api.parseStations(json).also { stationCache = it }
            }

            dataCache.readFallbackStations()?.let { json ->
                return api.parseStations(json).also { stationCache = it }
            }
        }

        return refreshStations()
    }

    override suspend fun refreshStations(): List<Station> {
        val json = api.fetchStationsJson()
        val stations = api.parseStations(json)
        dataCache.writeStations(json)
        return stations.also { stationCache = it }
    }

    override suspend fun getPlaces(forceRefresh: Boolean): List<PlaceGroup> {
        val cached = placeCache
        if (!forceRefresh && cached != null) return cached
        return api.fetchPlaces().also { placeCache = it }
    }

    override suspend fun getRouteOptions(fromStationCode: String, toStationCode: String): List<RoutePath> {
        val cacheKey = routeCacheKey(fromStationCode, toStationCode)
        val cached = routeCache[cacheKey]
        if (cached != null && Clock.System.now().toEpochMilliseconds() - cached.savedAt <= ROUTE_CACHE_MAX_AGE_MS) {
            return cached.routes
        }

        val routes = fetchRouteOptions(fromStationCode, toStationCode)
        if (routes.isNotEmpty()) {
            routeCache[cacheKey] = RouteCacheEntry(routes, Clock.System.now().toEpochMilliseconds())
        }
        return routes
    }

    private suspend fun fetchRouteOptions(
        fromStationCode: String,
        toStationCode: String,
    ): List<RoutePath> = coroutineScope {
        val allPathsResult = cancellableRunCatching {
            api.fetchAllPaths(fromStationCode, toStationCode, numPaths = 4)
        }
        val allPaths = allPathsResult.getOrDefault(emptyList())

        val fallbackPaths = if (allPaths.isEmpty()) {
            val cheapestPath = async {
                cancellableRunCatching { api.fetchCheapestPath(fromStationCode, toStationCode) }
            }
            val shortestPath = async {
                cancellableRunCatching { api.fetchShortestPath(fromStationCode, toStationCode) }
            }
            val cheapest = cheapestPath.await()
            val shortest = shortestPath.await()
            if (cheapest.isFailure && shortest.isFailure) {
                throw allPathsResult.exceptionOrNull() ?: cheapest.exceptionOrNull()!!
            }
            listOfNotNull(cheapest.getOrNull(), shortest.getOrNull())
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
