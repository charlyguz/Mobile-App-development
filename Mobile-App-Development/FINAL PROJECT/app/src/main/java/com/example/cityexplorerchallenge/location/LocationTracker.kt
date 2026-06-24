package com.example.cityexplorerchallenge.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationTracker(context: Context) {
    private companion object {
        private const val GOOGLEPLEX_LAT = 37.4219999
        private const val GOOGLEPLEX_LNG = -122.0840575
        private const val GOOGLEPLEX_TOLERANCE_METERS = 250f
        private const val ZERO_ZERO_TOLERANCE_METERS = 250f
        private const val MAX_LAST_KNOWN_AGE_MS = 2 * 60 * 1000L
    }

    private val appContext = context.applicationContext
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(appContext)

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _locationFlow.value = location
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val freshLocation = withTimeoutOrNull(15_000L) {
            requestFreshLocation(priority)
        }
        if (freshLocation != null && isUsableLocation(freshLocation)) {
            _locationFlow.value = freshLocation
            return freshLocation
        }

        val lastKnownLocation = getLastKnownLocation()
        if (lastKnownLocation != null && isRecent(lastKnownLocation) && isUsableLocation(lastKnownLocation)) {
            _locationFlow.value = lastKnownLocation
            return lastKnownLocation
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(priority: Int): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            val request = CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setDurationMillis(15_000L)
                .setMaxUpdateAgeMillis(0L)
                .build()

            try {
                fusedLocationClient.getCurrentLocation(
                    request,
                    cancellationTokenSource.token
                )
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isTracking || !hasLocationPermission()) return

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val locationRequest = LocationRequest.Builder(
            priority,
            10_000L // Update every 10 seconds
        ).setMinUpdateIntervalMillis(5_000L) // Fastest update every 5 seconds
            .setMinUpdateDistanceMeters(5f) // Only if moved 5 meters
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener {
                isTracking = false
            }
            isTracking = true
        } catch (e: SecurityException) {
            isTracking = false
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun isUsableLocation(location: Location): Boolean {
        return !isNear(location, 0.0, 0.0, ZERO_ZERO_TOLERANCE_METERS) &&
            !isNear(location, GOOGLEPLEX_LAT, GOOGLEPLEX_LNG, GOOGLEPLEX_TOLERANCE_METERS)
    }

    fun explainRejectedLocation(location: Location): String {
        return when {
            isNear(location, GOOGLEPLEX_LAT, GOOGLEPLEX_LNG, GOOGLEPLEX_TOLERANCE_METERS) ->
                "The emulator is reporting Android's default California location. Set a test location in Android Studio."
            isNear(location, 0.0, 0.0, ZERO_ZERO_TOLERANCE_METERS) ->
                "The device returned 0,0 instead of a real GPS fix. Enable location services and try again."
            else -> "The device location is not usable yet."
        }
    }

    fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() ||
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isRecent(location: Location): Boolean {
        return System.currentTimeMillis() - location.time <= MAX_LAST_KNOWN_AGE_MS
    }

    private fun isNear(location: Location, lat: Double, lng: Double, toleranceMeters: Float): Boolean {
        return calculateDistance(location.latitude, location.longitude, lat, lng) <= toleranceMeters
    }
}
