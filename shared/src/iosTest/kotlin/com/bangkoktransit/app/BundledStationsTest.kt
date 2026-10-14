package com.bangkoktransit.app

import com.bangkoktransit.app.data.api.BangkokRailwayApi
import com.bangkoktransit.app.data.local.readBundledStations
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundledStationsTest {
    @Test fun bundledStationsLoadWithoutNetworkAndContainPlannerStations() = runTest {
        val api = BangkokRailwayApi(client = HttpClient(MockEngine { error("Unexpected request") }))
        try {
            val stations = api.parseStations(readBundledStations())
            assertTrue(stations.size > 100)
            assertEquals("Siam", stations.single { it.stationCode == "CEN_NE" }.nameEn)
            assertEquals("Asok", stations.single { it.stationCode == "E04" }.nameEn)
        } finally { api.close() }
    }

}
