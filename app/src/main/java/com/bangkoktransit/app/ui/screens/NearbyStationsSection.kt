package com.bangkoktransit.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bangkoktransit.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.resume
import java.util.Locale

private sealed interface NearbyLocation {
    data object Permission : NearbyLocation
    data object Loading : NearbyLocation
    data object Disabled : NearbyLocation
    data object Unavailable : NearbyLocation
    data object Imprecise : NearbyLocation
    data class Ready(val location: Location) : NearbyLocation
}

@Composable
fun NearbyStationsSection(
    stations: List<Station>,
    onStationSelected: (Station) -> Unit,
    onRefreshStations: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<NearbyLocation>(NearbyLocation.Permission) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val hasCoordinates = stations.any { it.hasGpsCoordinates() }
    fun refresh() {
        job?.cancel()
        job = scope.launch {
            if (!context.hasLocationPermission()) {
                status = NearbyLocation.Permission
                return@launch
            }
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (manager == null) {
                status = NearbyLocation.Unavailable
                return@launch
            }
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                status = NearbyLocation.Disabled
                return@launch
            }
            status = NearbyLocation.Loading
            try {
                val fix = currentLocation(context, manager)
                status = when {
                    fix == null -> NearbyLocation.Unavailable
                    !fix.hasAccuracy() || !fix.accuracy.isFinite() || fix.accuracy > 1000f -> NearbyLocation.Imprecise
                    else -> NearbyLocation.Ready(fix)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SecurityException) {
                status = NearbyLocation.Permission
            } catch (_: Exception) {
                status = NearbyLocation.Unavailable
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh()
    }
    // Recheck after returning from settings, and stop work whenever this picker
    // leaves the foreground. Location is neither persisted nor sent to the API.
    DisposableEffect(lifecycle, hasCoordinates) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasCoordinates) refresh()
            if (event == Lifecycle.Event.ON_PAUSE) job?.cancel()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && hasCoordinates) refresh()
        onDispose { lifecycle.removeObserver(observer); job?.cancel() }
    }
    val ready = status as? NearbyLocation.Ready
    val nearby = remember(stations, ready) {
        ready?.let { nearbyStations(stations, it.location.latitude, it.location.longitude) }.orEmpty()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nearby stations", style = MaterialTheme.typography.titleSmall)
        when {
            !hasCoordinates -> {
                Text("Nearby suggestions are unavailable. Refresh station data or search below.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRefreshStations) { Text("Refresh stations") }
            }
            status == NearbyLocation.Permission -> {
                Text("Use your location to find stations within 2 km.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    if (requested) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")))
                    } else {
                        requested = true
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }) { Text(if (requested) "Location permission settings" else "Use my location") }
            }
            status == NearbyLocation.Loading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Finding your location…", style = MaterialTheme.typography.bodySmall)
                }
            }
            status == NearbyLocation.Disabled -> {
                Text("Turn on device location to find nearby stations.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("Location settings") }
            }
            status == NearbyLocation.Unavailable || status == NearbyLocation.Imprecise -> {
                Text(if (status == NearbyLocation.Imprecise) "Your location is too approximate to identify nearby stations. Enable precise location or try again."
                    else "Couldn't get your location. Try again or search below.", style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { refresh() }) { Text("Try again") }
                    if (status == NearbyLocation.Imprecise) TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }) { Text("Settings") }
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
                        Text(if (suggestion.distanceMeters < 1000) "${suggestion.distanceMeters.toInt()} m"
                            else String.format(Locale.getDefault(), "%.1f km", suggestion.distanceMeters / 1000),
                            style = MaterialTheme.typography.labelLarge)
                    })
                }
                TextButton(onClick = { refresh() }) { Text("Refresh location") }
            }
        }
    }
}

private fun Context.hasLocationPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // Checked immediately before the call; revocation is handled by caller.
private suspend fun currentLocation(context: Context, manager: LocationManager): Location? = coroutineScope {
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { manager.isProviderEnabled(it) }
    if (providers.isEmpty()) return@coroutineScope null
    val results = Channel<Location?>(Channel.UNLIMITED)
    val jobs = providers.map { provider ->
        launch {
            val fix = try {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(manager, provider, signal,
                        ContextCompat.getMainExecutor(context)) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
            results.send(fix)
        }
    }
    var best: Location? = null
    try {
        withTimeoutOrNull(15_000) {
            repeat(providers.size) {
                val fix = results.receive()
                if (fix != null && fix.hasAccuracy() && fix.accuracy.isFinite() &&
                    (SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) in 0L..120_000_000_000L) {
                    if (best == null || fix.accuracy < best!!.accuracy) best = fix
                    if (fix.accuracy <= 100f) return@withTimeoutOrNull
                }
            }
        }
        best
    } finally {
        jobs.forEach { it.cancel() }
        results.close()
    }
}
