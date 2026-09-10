package com.bangkoktransit.app.data.api

import com.bangkoktransit.app.data.model.FareBreakdownItem
import com.bangkoktransit.app.data.model.LineInfo
import com.bangkoktransit.app.data.model.PathStats
import com.bangkoktransit.app.data.model.PlaceGroup
import com.bangkoktransit.app.data.model.PlaceInfo
import com.bangkoktransit.app.data.model.RoutePath
import com.bangkoktransit.app.data.model.RouteStation
import com.bangkoktransit.app.data.model.RouteStep
import com.bangkoktransit.app.data.model.RouteStepStation
import com.bangkoktransit.app.data.model.Station
import com.bangkoktransit.app.data.model.StationLite
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class BangkokRailwayApi(
    baseUrl: String = "https://bangkok-railway-api.onrender.com",
    client: OkHttpClient = defaultClient(),
) {
    private val service = Retrofit.Builder()
        .baseUrl(baseUrl.normalizedBaseUrl())
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(BangkokRailwayService::class.java)

    suspend fun fetchStations(): List<Station> = withContext(Dispatchers.IO) {
        parseStations(fetchStationsJson())
    }

    suspend fun fetchStationsJson(): String {
        return request { getStations() }
    }

    fun parseStations(json: String): List<Station> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                add(parseStation(array.getJSONObject(index)))
            }
        }
    }

    suspend fun fetchPlaces(): List<PlaceGroup> = withContext(Dispatchers.IO) {
        val json = request { getPlaces() }
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                add(parsePlaceGroup(array.getJSONObject(index)))
            }
        }
    }

    suspend fun fetchAllPaths(
        fromStationCode: String,
        toStationCode: String,
        numPaths: Int = 3,
    ): List<RoutePath> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("from_station_code", fromStationCode)
            .put("to_station_code", toStationCode)
            .put("num_paths", numPaths)
        val response = JSONObject(request { getAllPaths(body.toJsonRequestBody()) })
        ensureSuccess(response)
        val data = response.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                add(parseRoutePath(data.getJSONObject(index)))
            }
        }
    }

    suspend fun fetchShortestPath(fromStationCode: String, toStationCode: String): RoutePath =
        withContext(Dispatchers.IO) {
            val body = routeBody(fromStationCode, toStationCode)
            val response = JSONObject(request { getShortestPath(body.toJsonRequestBody()) })
            parseSinglePathResponse(response)
        }

    suspend fun fetchCheapestPath(fromStationCode: String, toStationCode: String): RoutePath =
        withContext(Dispatchers.IO) {
            val body = routeBody(fromStationCode, toStationCode)
            val response = JSONObject(request { getCheapestPath(body.toJsonRequestBody()) })
            parseSinglePathResponse(response)
        }

    private fun parseSinglePathResponse(response: JSONObject): RoutePath {
        ensureSuccess(response)
        val data = response.optJSONObject("data")
            ?: throw IOException(response.optString("message", "Route response did not include data."))
        return parseRoutePath(data)
    }

    private suspend fun request(call: suspend BangkokRailwayService.() -> Response<String>): String {
        val response = try {
            service.call()
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException(error.message ?: "API request failed.", error)
        }

        if (response.isSuccessful) {
            return response.body().orEmpty()
        }

        val responseText = response.errorBody()?.string().orEmpty()
        throw IOException("HTTP ${response.code()} ${response.message()}: $responseText")
    }

    private fun routeBody(fromStationCode: String, toStationCode: String): JSONObject {
        return JSONObject()
            .put("from_station_code", fromStationCode)
            .put("to_station_code", toStationCode)
    }

    private fun JSONObject.toJsonRequestBody(): RequestBody {
        return toString().toRequestBody(JSON_MEDIA_TYPE)
    }

    private fun ensureSuccess(response: JSONObject) {
        val status = response.optString("status")
        if (status.equals("success", ignoreCase = true)) return
        val error = response.optNullableString("error")
            ?: response.optNullableString("message")
            ?: "API request failed."
        throw IOException(error)
    }

    private fun parseStation(json: JSONObject): Station {
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

    private fun parseLine(json: JSONObject): LineInfo {
        return LineInfo(
            id = json.optInt("id"),
            nameEn = json.optString("name_en"),
            nameThai = json.optNullableString("name_thai"),
        )
    }

    private fun parsePlace(json: JSONObject): PlaceInfo {
        return PlaceInfo(
            id = json.optInt("id"),
            nameEn = json.optString("name_en"),
            nameThai = json.optNullableString("name_thai"),
        )
    }

    private fun parsePlaceGroup(json: JSONObject): PlaceGroup {
        val stationsArray = json.optJSONArray("stations") ?: JSONArray()
        val stations = buildList {
            for (index in 0 until stationsArray.length()) {
                val stationJson = stationsArray.getJSONObject(index)
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

    private fun parseRoutePath(json: JSONObject): RoutePath {
        val statsJson = json.optJSONObject("stats") ?: JSONObject()
        val routeStepsArray = json.optJSONArray("route_steps") ?: JSONArray()
        val stationsArray = json.optJSONArray("stations") ?: JSONArray()
        val fareBreakdownArray = json.optJSONArray("fare_breakdown") ?: JSONArray()

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
                for (index in 0 until routeStepsArray.length()) {
                    val stepJson = routeStepsArray.getJSONObject(index)
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
                for (index in 0 until stationsArray.length()) {
                    val stationJson = stationsArray.getJSONObject(index)
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
                for (index in 0 until fareBreakdownArray.length()) {
                    val fareJson = fareBreakdownArray.getJSONObject(index)
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

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optDouble(name)
    }

    private interface BangkokRailwayService {
        @GET("stations/")
        suspend fun getStations(): Response<String>

        @GET("places/")
        suspend fun getPlaces(): Response<String>

        @POST("paths/all_paths")
        suspend fun getAllPaths(@Body body: RequestBody): Response<String>

        @POST("paths/shortest")
        suspend fun getShortestPath(@Body body: RequestBody): Response<String>

        @POST("paths/cheapest")
        suspend fun getCheapestPath(@Body body: RequestBody): Response<String>
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun String.normalizedBaseUrl(): String {
            val trimmed = trim()
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}
