package com.transitgo.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.bangkoktransit.app.data.api.BangkokRailwayApi
import com.bangkoktransit.app.data.local.IosStringPreferences
import com.bangkoktransit.app.data.local.IosTransitDataCache
import com.bangkoktransit.app.data.local.TripStore
import com.bangkoktransit.app.data.repository.TransitRepository
import com.bangkoktransit.app.domain.usecase.*
import com.bangkoktransit.app.ui.viewmodel.TransitController
import com.bangkoktransit.app.ui.screens.IosNearbyStationsSection
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val preferences = NSUserDefaults.standardUserDefaults
    val savedDarkMode = if (preferences.objectForKey("dark_mode") != null) {
        preferences.boolForKey("dark_mode")
    } else {
        null
    }
    val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0"
    var viewController: UIViewController? = null

    return ComposeUIViewController {
        val scope = rememberCoroutineScope()
        val api = remember { BangkokRailwayApi() }
        val controller = remember {
            val repository = TransitRepository(IosTransitDataCache(), api)
            TransitController(
                scope = scope,
                getStations = GetStations(repository),
                refreshStations = RefreshStations(repository),
                getPlaces = GetPlaces(repository),
                getRouteOptions = GetRouteOptions(repository),
                savedTrips = ManageSavedTrips(TripStore(IosStringPreferences("transit_trips"))),
            )
        }
        DisposableEffect(api) {
            onDispose { api.close() }
        }
        val systemUsesDarkTheme = isSystemInDarkTheme()
        var darkModeEnabled by rememberSaveable {
            mutableStateOf(savedDarkMode ?: systemUsesDarkTheme)
        }
        SideEffect {
            viewController?.overrideUserInterfaceStyle = if (darkModeEnabled) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark
            } else {
                UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }
        }
        SharedTransitApp(
            controller = controller,
            versionName = versionName,
            darkModeEnabled = darkModeEnabled,
            nearbyContent = { stations, onSelected, onRefresh ->
                IosNearbyStationsSection(stations, onSelected, onRefresh)
            },
            onDarkModeChanged = { enabled ->
                darkModeEnabled = enabled
                preferences.setBool(enabled, forKey = "dark_mode")
            },
        )
    }.also { viewController = it }
}
