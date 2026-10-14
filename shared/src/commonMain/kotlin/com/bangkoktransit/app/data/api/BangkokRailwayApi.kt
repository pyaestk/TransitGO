package com.bangkoktransit.app.data.api

import com.bangkoktransit.app.domain.model.FareBreakdownItem
import com.bangkoktransit.app.domain.model.LineInfo
import com.bangkoktransit.app.domain.model.PathStats
import com.bangkoktransit.app.domain.model.PlaceGroup
import com.bangkoktransit.app.domain.model.PlaceInfo
import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.RouteStation
import com.bangkoktransit.app.domain.model.RouteStep
import com.bangkoktransit.app.domain.model.RouteStepStation
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.domain.model.StationLite
import io.ktor.client.HttpClient
import io.ktor.http.contentType
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

class TransitApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BangkokRailwayApi(
    baseUrl: String = "https://bangkok-railway-api.onrender.com",
    private val client: HttpClient = createTransitHttpClient(),
) {
    private val baseUrl = baseUrl.trim().trimEnd('/') + "/"

    suspend fun fetchStations(): List<Station> = parseStations(fetchStationsJson())

    suspend fun fetchStationsJson(): String = request("stations/")

    fun parseStations(json: String): List<Station> =
        Json.parseToJsonElement(json).jsonArray.map { parseStation(it.jsonObject) }

    suspend fun fetchPlaces(): List<PlaceGroup> =
        Json.parseToJsonElement(request("places/")).jsonArray.map { parsePlaceGroup(it.jsonObject) }

    suspend fun fetchAllPaths(
        fromStationCode: String,
        toStationCode: String,
        numPaths: Int = 3,
    ): List<RoutePath> {
        val body = buildJsonObject {
            put("from_station_code", fromStationCode)
            put("to_station_code", toStationCode)
            put("num_paths", numPaths)
        }
        val response = Json.parseToJsonElement(request("paths/all_paths", body)).jsonObject
        ensureSuccess(response)
        return response.optJSONArray("data").orEmpty().map { parseRoutePath(it.jsonObject) }
    }

    suspend fun fetchShortestPath(fromStationCode: String, toStationCode: String): RoutePath =
        fetchSinglePath("paths/shortest", fromStationCode, toStationCode)

    suspend fun fetchCheapestPath(fromStationCode: String, toStationCode: String): RoutePath =
        fetchSinglePath("paths/cheapest", fromStationCode, toStationCode)

    private suspend fun fetchSinglePath(path: String, fromCode: String, toCode: String): RoutePath {
        val body = buildJsonObject {
            put("from_station_code", fromCode)
            put("to_station_code", toCode)
        }
        val response = Json.parseToJsonElement(request(path, body)).jsonObject
        ensureSuccess(response)
        val data = response.optJSONObject("data")
            ?: throw TransitApiException(response.optNullableString("message") ?: "Route response did not include data.")
        return parseRoutePath(data)
    }

    private suspend fun request(path: String, body: JsonObject? = null): String {
        try {
            val response = client.request(baseUrl + path) {
                method = if (body == null) HttpMethod.Get else HttpMethod.Post
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
            val text = response.bodyAsText()
            if (response.status.value !in 200..299) {
                throw TransitApiException("HTTP ${response.status.value}: $text")
            }
            return text
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: TransitApiException) {
            throw error
        } catch (error: Exception) {
            throw TransitApiException(error.message ?: "API request failed.", error)
        }
    }

    fun close() = client.close()

    private fun ensureSuccess(response: JsonObject) {
        val status = response.optString("status")
        if (status.equals("success", ignoreCase = true)) return
        val error = response.optNullableString("error")
            ?: response.optNullableString("message")
            ?: "API request failed."
        throw TransitApiException(error)
    }

    private fun parseStation(json: JsonObject): Station {
        return Station(
            id = json.optInt("id"),
            stationCode = json.optString("station_code"),
            stationShortName = json.optString("station_short_name"),
            nameEn = json.optString("name_en"),
            nameThai = json.optNullableString("name_thai"),
            line = json.optJSONObject("line")?.let(::parseLine),
            place = json.optJSONObject("place")?.let(::parsePlace),
            x = json.optDouble("x"),
            y = json.optDouble("y"),
            latitude = json.optDouble("latitude").takeIf { it.isFinite() && it in -90.0..90.0 },
            longitude = json.optDouble("longitude").takeIf { it.isFinite() && it in -180.0..180.0 },
        )
    }

    private fun parseLine(json: JsonObject): LineInfo {
        return LineInfo(
            id = json.optInt("id"),
            nameEn = json.optString("name_en"),
            nameThai = json.optNullableString("name_thai"),
        )
    }

    private fun parsePlace(json: JsonObject): PlaceInfo {
        return PlaceInfo(
            id = json.optInt("id"),
            nameEn = json.optString("name_en"),
            nameThai = json.optNullableString("name_thai"),
        )
    }

    private fun parsePlaceGroup(json: JsonObject): PlaceGroup {
        val stationsArray = json.optJSONArray("stations") ?: JsonArray(emptyList())
        val stations = buildList {
            for (index in 0 until stationsArray.size) {
                val stationJson = stationsArray[index].jsonObject
                add(
                    StationLite(
                        id = stationJson.optInt("id"),
                        stationCode = stationJson.optString("station_code"),
                        stationShortName = stationJson.optString("station_short_name"),
                        nameEn = stationJson.optString("name_en"),
                        nameThai = stationJson.optNullableString("name_thai"),
                    ),
                )
            }
        }

        return PlaceGroup(
            id = json.optInt("id"),
            nameEn = json.optString("name_en"),
            nameThai = json.optNullableString("name_thai"),
            stations = stations,
        )
    }

    private fun parseRoutePath(json: JsonObject): RoutePath {
        val statsJson = json.optJSONObject("stats") ?: JsonObject(emptyMap())
        val routeStepsArray = json.optJSONArray("route_steps") ?: JsonArray(emptyList())
        val stationsArray = json.optJSONArray("stations") ?: JsonArray(emptyList())
        val fareBreakdownArray = json.optJSONArray("fare_breakdown") ?: JsonArray(emptyList())

        return RoutePath(
            pathType = json.optString("path_type"),
            startStationCode = json.optString("start_station_code"),
            endStationCode = json.optString("end_station_code"),
            stats = PathStats(
                totalStations = statsJson.optInt("total_stations"),
                totalTransfers = statsJson.optInt("total_transfers"),
                totalLines = statsJson.optInt("total_lines"),
            ),
            routeDescription = json.optString("route_description"),
            routeSteps = buildList {
                for (index in 0 until routeStepsArray.size) {
                    val stepJson = routeStepsArray[index].jsonObject
                    val stationJson = stepJson.optJSONObject("station")
                    add(
                        RouteStep(
                            action = stepJson.optString("action"),
                            line = stepJson.optNullableString("line"),
                            station = stationJson?.let {
                                RouteStepStation(
                                    code = it.optString("code"),
                                    name = it.optString("name"),
                                )
                            },
                        ),
                    )
                }
            },
            stations = buildList {
                for (index in 0 until stationsArray.size) {
                    val stationJson = stationsArray[index].jsonObject
                    add(
                        RouteStation(
                            stationCode = stationJson.optString("station_code"),
                            x = stationJson.optDouble("x"),
                            y = stationJson.optDouble("y"),
                        ),
                    )
                }
            },
            fareTotal = json.optDoubleOrNull("fare_total"),
            fareBreakdown = buildList {
                for (index in 0 until fareBreakdownArray.size) {
                    val fareJson = fareBreakdownArray[index].jsonObject
                    add(
                        FareBreakdownItem(
                            agency = fareJson.optString("agency"),
                            rideHops = fareJson.optInt("ride_hops"),
                            cost = fareJson.optDouble("cost"),
                        ),
                    )
                }
            },
        )
    }

}

internal fun JsonObject.optString(name: String): String =
    (this[name] as? JsonPrimitive)?.contentOrNull.orEmpty()

internal fun JsonObject.optNullableString(name: String): String? =
    optString(name).takeIf { it.isNotBlank() && it != "null" }

internal fun JsonObject.optInt(name: String): Int =
    (this[name] as? JsonPrimitive)?.intOrNull ?: 0

internal fun JsonObject.optDouble(name: String): Double = optDoubleOrNull(name) ?: Double.NaN

internal fun JsonObject.optDoubleOrNull(name: String): Double? =
    (this[name] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

internal fun JsonObject.optJSONObject(name: String): JsonObject? = this[name] as? JsonObject

internal fun JsonObject.optJSONArray(name: String): JsonArray? = this[name] as? JsonArray
