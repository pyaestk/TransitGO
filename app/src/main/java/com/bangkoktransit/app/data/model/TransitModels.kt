package com.bangkoktransit.app.data.model

data class LineInfo(
    val id: Int,
    val nameEn: String,
    val nameThai: String? = null,
)

data class PlaceInfo(
    val id: Int,
    val nameEn: String,
    val nameThai: String? = null,
)

data class Station(
    val id: Int,
    val stationCode: String,
    val stationShortName: String,
    val nameEn: String,
    val nameThai: String? = null,
    val line: LineInfo? = null,
    val place: PlaceInfo? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val displayCode: String
        get() = stationShortName.ifBlank { stationCode }
}

data class PlaceGroup(
    val id: Int,
    val nameEn: String,
    val nameThai: String? = null,
    val stations: List<StationLite> = emptyList(),
)

data class StationLite(
    val id: Int,
    val stationCode: String,
    val stationShortName: String,
    val nameEn: String,
    val nameThai: String? = null,
)

data class PathStats(
    val totalStations: Int,
    val totalTransfers: Int,
    val totalLines: Int,
)

data class RouteStepStation(
    val code: String,
    val name: String,
)

data class RouteStep(
    val action: String,
    val line: String? = null,
    val station: RouteStepStation? = null,
)

data class RouteStation(
    val stationCode: String,
    val x: Double,
    val y: Double,
)

data class FareBreakdownItem(
    val agency: String,
    val rideHops: Int,
    val cost: Double,
)

data class RoutePath(
    val pathType: String,
    val startStationCode: String,
    val endStationCode: String,
    val stats: PathStats,
    val routeDescription: String,
    val routeSteps: List<RouteStep>,
    val stations: List<RouteStation>,
    val fareTotal: Double? = null,
    val fareBreakdown: List<FareBreakdownItem> = emptyList(),
) {
    val routeKey: String
        get() = buildString {
            append(pathType)
            append(":")
            append(stations.joinToString(">") { it.stationCode })
        }
}

data class SavedTrip(
    val fromCode: String,
    val fromName: String,
    val toCode: String,
    val toName: String,
    val routeType: String,
    val fareTotal: Double?,
    val totalStations: Int,
    val totalTransfers: Int,
    val savedAt: Long,
) {
    val key: String
        get() = "$fromCode:$toCode:$routeType"
}

enum class RoutePreference(val title: String, val subtitle: String) {
    Recommended("Recommended", "Best overall"),
    Cheapest("Cheapest", "Lowest fare"),
    FewestStations("Fewest stations", "Shortest ride"),
    FewestTransfers("Fewest transfers", "Simpler trip"),
}
