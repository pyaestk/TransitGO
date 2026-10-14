package com.bangkoktransit.app

import com.bangkoktransit.app.data.api.BangkokRailwayApi
import com.bangkoktransit.app.data.api.TransitApiException
import com.bangkoktransit.app.data.local.StationDataCache
import com.bangkoktransit.app.data.repository.TransitRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

private val jsonHeaders = headersOf("Content-Type", "application/json")

internal const val STATIONS_JSON = """[
  {"id":172,"station_code":"CEN_NE","station_short_name":"CEN","name_en":"Siam",
   "name_thai":"สยาม","x":430.82,"y":508.89,"latitude":"13.7456","longitude":100.5346,
   "line":{"id":1,"name_en":"BTS Sukhumvit Line"},"place":{"id":1,"name_en":"Siam"}},
  {"id":176,"station_code":"E04","station_short_name":"E4","name_en":"Asok",
   "x":499.83,"y":512.74,"latitude":95,"longitude":null,"name_thai":null}
]"""

internal const val ROUTE_JSON = """{
  "path_type":"cheapest","start_station_code":"CEN_NE","end_station_code":"E04",
  "stats":{"total_stations":5,"total_transfers":0,"total_lines":1},
  "route_description":"Siam to Asok",
  "route_steps":[{"action":"Board","line":"BTS Sukhumvit Line","station":{"code":"CEN_NE","name":"Siam"}},
                 {"action":"Arrive","station":{"code":"E04","name":"Asok"}}],
  "stations":[{"station_code":"CEN_NE","x":430.82,"y":508.89},{"station_code":"E04","x":499.83,"y":512.74}],
  "fare_total":35.5,"fare_breakdown":[{"agency":"BTS","ride_hops":4,"cost":35.5}]
}"""

internal class TestStationCache(
    var cachedJson: String? = null,
    private val fallbackJson: String? = STATIONS_JSON,
) : StationDataCache {
    override fun readStations(maxAgeMillis: Long): String? = cachedJson
    override fun writeStations(json: String) { cachedJson = json }
    override suspend fun readFallbackStations(): String? = fallbackJson
}

class TransitNetworkingTest {
    @Test fun stationParsingKeepsGpsSeparateFromMapCoordinates() {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine { error("Unexpected request") }))
        try {
            val stations = api.parseStations(STATIONS_JSON)
            assertEquals("CEN", stations[0].displayCode)
            assertEquals("สยาม", stations[0].nameThai)
            assertEquals(13.7456, stations[0].latitude)
            assertEquals("BTS Sukhumvit Line", stations[0].line?.nameEn)
            assertEquals("Siam", stations[0].place?.nameEn)
            assertNull(stations[1].latitude)
            assertNull(stations[1].longitude)
            assertNull(stations[1].nameThai)
            assertEquals(499.83, stations[1].x)
        } finally { api.close() }
    }

    @Test fun routeRequestUsesBackendContractAndParsesFaresAndSteps() = runTest {
        val api = BangkokRailwayApi("https://test.example/", HttpClient(MockEngine { request ->
            assertEquals("/paths/all_paths", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("CEN_NE", body["from_station_code"]?.jsonPrimitive?.content)
            assertEquals("E04", body["to_station_code"]?.jsonPrimitive?.content)
            assertEquals(4, body["num_paths"]?.jsonPrimitive?.int)
            respond("""{"status":"success","data":[$ROUTE_JSON]}""", headers = jsonHeaders)
        }))
        try {
            val route = api.fetchAllPaths("CEN_NE", "E04", 4).single()
            assertEquals(35.5, route.fareTotal)
            assertEquals(35.5, route.fareBreakdown.single().cost)
            assertEquals(5, route.stats.totalStations)
            assertEquals("E04", route.routeSteps.last().station?.code)
            assertEquals(listOf("CEN_NE", "E04"), route.stations.map { it.stationCode })
        } finally { api.close() }
    }

    @Test fun placesKeepTheirStationGroups() = runTest {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine { request ->
            assertEquals("/places/", request.url.encodedPath)
            respond("""[{"id":1,"name_en":"Siam","stations":[{"id":172,"station_code":"CEN_NE","station_short_name":"CEN","name_en":"Siam"}]}]""", headers = jsonHeaders)
        }))
        try {
            val place = api.fetchPlaces().single()
            assertEquals("Siam", place.nameEn)
            assertEquals("CEN_NE", place.stations.single().stationCode)
        } finally { api.close() }
    }

    @Test fun backendErrorIsReportedAsARouteError() = runTest {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine {
            respond("""{"status":"error","message":"Station code was not found"}""", headers = jsonHeaders)
        }))
        try {
            val error = assertFailsWith<TransitApiException> { api.fetchAllPaths("bad", "E04") }
            assertEquals("Station code was not found", error.message)
        } finally { api.close() }
    }

    @Test fun httpFailuresDoNotBecomeSuccessfulEmptyData() = runTest {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine {
            respond("Service unavailable", HttpStatusCode.ServiceUnavailable)
        }))
        try {
            val error = assertFailsWith<TransitApiException> { api.fetchStations() }
            assertTrue(error.message.orEmpty().contains("503"))
        } finally { api.close() }
    }

    @Test fun cancellationIsNotConvertedIntoAnApiFailure() = runTest {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine { throw CancellationException("Cancelled route") }))
        try {
            assertFailsWith<CancellationException> { api.fetchAllPaths("CEN_NE", "E04") }
        } finally { api.close() }
    }

    @Test fun fallbackRoutesAreDeduplicatedAndCached() = runTest {
        val paths = mutableListOf<String>()
        val api = BangkokRailwayApi(client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            if (request.url.encodedPath == "/paths/all_paths") {
                respond("Unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond("""{"status":"success","data":$ROUTE_JSON}""", headers = jsonHeaders)
            }
        }))
        try {
            val repository = TransitRepository(TestStationCache(), api)
            assertEquals(1, repository.getRouteOptions("CEN_NE", "E04").size)
            assertEquals(setOf("/paths/all_paths", "/paths/cheapest", "/paths/shortest"), paths.toSet())
            assertEquals(1, repository.getRouteOptions("CEN_NE", "E04").size)
            assertEquals(3, paths.size)
        } finally { api.close() }
    }

    @Test fun malformedRefreshDoesNotOverwritePreviouslyValidStationCache() = runTest {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine { respond("not json") }))
        try {
            val cache = TestStationCache(cachedJson = STATIONS_JSON)
            val repository = TransitRepository(cache, api)
            assertEquals(2, repository.getStations().size)
            assertFails { repository.refreshStations() }
            assertEquals(STATIONS_JSON, cache.cachedJson)
            assertEquals(2, repository.getStations().size)
        } finally { api.close() }
    }
}
