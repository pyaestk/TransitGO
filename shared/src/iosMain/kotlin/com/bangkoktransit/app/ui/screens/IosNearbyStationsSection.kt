@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bangkoktransit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.domain.model.hasGpsCoordinates
import com.bangkoktransit.app.domain.model.nearbyStations
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreLocation.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.math.roundToInt

private sealed interface IosNearbyLocation {
    data object Permission : IosNearbyLocation
    data object Denied : IosNearbyLocation
    data object Loading : IosNearbyLocation
    data object Disabled : IosNearbyLocation
    data object Unavailable : IosNearbyLocation
    data object Imprecise : IosNearbyLocation
    data class Ready(val latitude: Double, val longitude: Double) : IosNearbyLocation
}

/** A foreground-only, one-shot fix. Coordinates are never persisted or sent to the API. */
private class IosNearbyLocationProvider(private val scope: CoroutineScope) :
    NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var timeout: Job? = null
    private var enabled = false
    private var foreground = false
    var status by mutableStateOf<IosNearbyLocation>(IosNearbyLocation.Permission)
        private set

    init {
        manager.delegate = this
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    fun activate() {
        enabled = true
        foreground = UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
        if (foreground) refresh()
    }

    fun resume() {
        foreground = true
        if (enabled) refresh()
    }

    fun pause() {
        foreground = false
        stopRequest()
        // Do not retain a fix while the app is away or after the picker closes.
        status = IosNearbyLocation.Permission
    }

    fun requestPermission() {
        if (!enabled || !foreground) return
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
            manager.requestWhenInUseAuthorization()
        } else {
            refresh()
        }
    }

    fun refresh() {
        if (!enabled || !foreground || status == IosNearbyLocation.Loading) return
        stopRequest()
        status = when (manager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> IosNearbyLocation.Permission
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted ->
                if (CLLocationManager.locationServicesEnabled()) IosNearbyLocation.Denied else IosNearbyLocation.Disabled
            kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> {
                when {
                    !CLLocationManager.locationServicesEnabled() -> IosNearbyLocation.Disabled
                    manager.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy -> IosNearbyLocation.Imprecise
                    else -> IosNearbyLocation.Loading
                }
            }
            else -> IosNearbyLocation.Unavailable
        }
        if (status == IosNearbyLocation.Loading) {
            timeout = scope.launch {
                delay(15_000)
                manager.stopUpdatingLocation()
                status = IosNearbyLocation.Unavailable
            }
            manager.requestLocation()
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        if (enabled && foreground) {
            stopRequest()
            status = IosNearbyLocation.Permission
            refresh()
        }
    }

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        if (!enabled || !foreground || status != IosNearbyLocation.Loading) return
        stopRequest()
        val now = NSDate().timeIntervalSince1970
        val fix = didUpdateLocations.filterIsInstance<CLLocation>()
            .filter { now - it.timestamp.timeIntervalSince1970 in -5.0..120.0 }
            .filter { it.horizontalAccuracy.isFinite() && it.horizontalAccuracy >= 0 }
            .minByOrNull { it.horizontalAccuracy }
        status = when {
            fix == null -> IosNearbyLocation.Unavailable
            fix.horizontalAccuracy > 1000 -> IosNearbyLocation.Imprecise
            else -> fix.coordinate.useContents {
                if (latitude.isFinite() && latitude in -90.0..90.0 &&
                    longitude.isFinite() && longitude in -180.0..180.0) {
                    IosNearbyLocation.Ready(latitude, longitude)
                } else {
                    IosNearbyLocation.Unavailable
                }
            }
        }
    }

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        if (!enabled || !foreground || status != IosNearbyLocation.Loading) return
        stopRequest()
        status = if (manager.authorizationStatus == kCLAuthorizationStatusDenied) {
            IosNearbyLocation.Denied
        } else {
            IosNearbyLocation.Unavailable
        }
    }

    private fun stopRequest() {
        timeout?.cancel()
        timeout = null
        manager.stopUpdatingLocation()
    }

    fun dispose() {
        enabled = false
        pause()
        manager.delegate = null
    }
}

@Composable
fun IosNearbyStationsSection(
    stations: List<Station>,
    onStationSelected: (Station) -> Unit,
    onRefreshStations: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val provider = remember { IosNearbyLocationProvider(scope) }
    val hasCoordinates = stations.any { it.hasGpsCoordinates() }
    DisposableEffect(provider, hasCoordinates) {
        val center = NSNotificationCenter.defaultCenter
        val resume = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null,
            NSOperationQueue.mainQueue) { if (hasCoordinates) provider.resume() }
        val pause = center.addObserverForName(UIApplicationWillResignActiveNotification, null,
            NSOperationQueue.mainQueue) { provider.pause() }
        if (hasCoordinates) provider.activate()
        onDispose {
            center.removeObserver(resume)
            center.removeObserver(pause)
            provider.pause()
        }
    }
    DisposableEffect(provider) { onDispose { provider.dispose() } }

    val status = provider.status
    val ready = status as? IosNearbyLocation.Ready
    val nearby = remember(stations, ready) {
        ready?.let { nearbyStations(stations, it.latitude, it.longitude) }.orEmpty()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nearby stations", style = MaterialTheme.typography.titleSmall)
        when {
            !hasCoordinates -> {
                Text("Nearby suggestions are unavailable. Refresh station data or search below.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRefreshStations) { Text("Refresh stations") }
            }
            status == IosNearbyLocation.Permission -> {
                Text("Use your location to find stations within 2 km.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = provider::requestPermission) { Text("Use my location") }
            }
            status == IosNearbyLocation.Loading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Finding your location…", style = MaterialTheme.typography.bodySmall)
                }
            }
            status == IosNearbyLocation.Denied || status == IosNearbyLocation.Disabled -> {
                Text(if (status == IosNearbyLocation.Disabled) "Turn on Location Services in iPhone Settings to find nearby stations."
                    else "Allow location access for TransitGO in Settings, or search below.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = ::openLocationSettings) { Text("Location settings") }
            }
            status == IosNearbyLocation.Unavailable || status == IosNearbyLocation.Imprecise -> {
                Text(if (status == IosNearbyLocation.Imprecise) "Your location is too approximate to identify nearby stations. Enable Precise Location or search below."
                    else "Couldn't get your location. Try again or search below.",
                    style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = provider::refresh) { Text("Try again") }
                    if (status == IosNearbyLocation.Imprecise) {
                        TextButton(onClick = ::openLocationSettings) { Text("Settings") }
                    }
                }
            }
            ready != null -> {
                Text(if (nearby.isEmpty()) "No nearby station within 2 km. Search for a station below."
                    else "Within 2 km · approximate straight-line distance", style = MaterialTheme.typography.bodySmall)
                if (stations.any { !it.hasGpsCoordinates() }) {
                    Text("Some stations don't have location data yet.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                nearby.forEach { suggestion ->
                    StationRow(station = suggestion.station, onClick = { onStationSelected(suggestion.station) }, trailing = {
                        val tenthsKm = (suggestion.distanceMeters / 100).roundToInt()
                        Text(if (suggestion.distanceMeters < 1000) "${suggestion.distanceMeters.toInt()} m"
                            else "${tenthsKm / 10}.${tenthsKm % 10} km", style = MaterialTheme.typography.labelLarge)
                    })
                }
                TextButton(onClick = provider::refresh) { Text("Refresh location") }
            }
        }
    }
}

private fun openLocationSettings() {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
        UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
