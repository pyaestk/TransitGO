package com.bangkoktransit.app

import com.bangkoktransit.app.data.local.TripStore
import com.bangkoktransit.app.domain.model.*
import com.bangkoktransit.app.domain.repository.TransitRepository
import com.bangkoktransit.app.domain.usecase.*
import com.bangkoktransit.app.ui.viewmodel.TransitController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TransitControllerTest {
    @Test fun changingStationsCancelsOldRouteAndPreventsStaleResults() = runTest {
        val routeResult = CompletableDeferred<List<RoutePath>>()
        val repository = TestTransitRepository { routeResult.await() }
        val controller = controller(repository)
        runCurrent()
        controller.selectStart(testStation("CEN_NE"))
        controller.selectTarget(testStation("E04"))
        controller.planRoute()
        runCurrent()
        assertTrue(controller.uiState.value.isPlanningRoute)
        controller.swapStations()
        routeResult.complete(listOf(testRoute(35.5)))
        runCurrent()
        val state = controller.uiState.value
        assertEquals("E04", state.selectedStart?.stationCode)
        assertFalse(state.isPlanningRoute)
        assertNull(state.activeRoute)
        assertTrue(state.recentTrips.isEmpty())
    }

    @Test fun rapidPlanTapsMakeOneRequestAndResultCanBeSaved() = runTest {
        var requestCount = 0
        val repository = TestTransitRepository {
            requestCount++
            listOf(testRoute(35.5))
        }
        val controller = controller(repository)
        runCurrent()
        controller.selectStart(testStation("CEN_NE"))
        controller.selectTarget(testStation("E04"))
        controller.planRoute()
        controller.planRoute()
        runCurrent()
        assertEquals(1, requestCount)
        assertEquals(35.5, controller.uiState.value.activeRoute?.fareTotal)
        assertEquals(1, controller.uiState.value.recentTrips.size)
        controller.toggleSavedActiveRoute()
        assertTrue(controller.uiState.value.isActiveRouteSaved)
        assertEquals(1, controller.uiState.value.savedTrips.size)
    }

    @Test fun sameStationIsRejectedBeforeCallingTheApi() = runTest {
        var called = false
        val controller = controller(TestTransitRepository { called = true; emptyList() })
        runCurrent()
        controller.selectStart(testStation("CEN_NE"))
        controller.selectTarget(testStation("CEN_NE"))
        controller.planRoute()
        runCurrent()
        assertFalse(called)
        assertEquals("Start and destination must be different.", controller.uiState.value.routeError)
    }

    private fun TestScope.controller(repository: TransitRepository): TransitController = TransitController(
        backgroundScope,
        GetStations(repository), RefreshStations(repository), GetPlaces(repository), GetRouteOptions(repository),
        ManageSavedTrips(TripStore(TestPreferences())),
    )

    private class TestTransitRepository(private val routes: suspend () -> List<RoutePath>) : TransitRepository {
        override suspend fun getStations(forceRefresh: Boolean) = listOf(testStation("CEN_NE"), testStation("E04"))
        override suspend fun refreshStations() = getStations(false)
        override suspend fun getPlaces(forceRefresh: Boolean) = emptyList<PlaceGroup>()
        override suspend fun getRouteOptions(fromStationCode: String, toStationCode: String) = routes()
    }
}
