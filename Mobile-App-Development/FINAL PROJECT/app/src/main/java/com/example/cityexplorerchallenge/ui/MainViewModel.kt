package com.example.cityexplorerchallenge.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityexplorerchallenge.data.local.AppDatabase
import com.example.cityexplorerchallenge.data.local.ChallengeEntity
import com.example.cityexplorerchallenge.data.remote.Place
import com.example.cityexplorerchallenge.data.remote.PlacesRepository
import com.example.cityexplorerchallenge.domain.ChallengeGenerator
import com.example.cityexplorerchallenge.location.LocationTracker
import com.example.cityexplorerchallenge.notifications.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val COMPLETION_RADIUS_METERS = 150
    }

    private val challengeCategories = listOf("Nature", "Culture", "Food", "Sport")
    private val searchRadiiMeters = listOf(1500, 3000, 5000)

    private val db = AppDatabase.getDatabase(application)
    private val challengeDao = db.challengeDao()
    private val repository = PlacesRepository()
    private val generator = ChallengeGenerator()
    private val notificationHelper = NotificationHelper(application)
    val locationTracker = LocationTracker(application)

    private val _activeChallenge = MutableStateFlow<ChallengeEntity?>(null)
    val activeChallenge: StateFlow<ChallengeEntity?> = _activeChallenge.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _locationStatus = MutableStateFlow("Location permission is needed for dynamic missions.")
    val locationStatus: StateFlow<String> = _locationStatus.asStateFlow()

    val historyFlow = challengeDao.getHistoryFlow()

    private var waitingForFirstLocationMission = false

    init {
        viewModelScope.launch {
            challengeDao.getActiveChallengeFlow().collect {
                _activeChallenge.value = it
            }
        }

        viewModelScope.launch {
            locationTracker.locationFlow.collect { location ->
                if (location != null) {
                    if (locationTracker.isUsableLocation(location)) {
                        _currentLocation.value = location
                        _locationStatus.value = formatLocationStatus(location)
                        if (waitingForFirstLocationMission && _activeChallenge.value == null) {
                            waitingForFirstLocationMission = false
                            generateNewChallenge()
                        }
                    } else {
                        _currentLocation.value = null
                        _locationStatus.value = locationTracker.explainRejectedLocation(location)
                    }
                }
            }
        }
    }

    fun startTracking() {
        locationTracker.startLocationUpdates()
    }

    fun stopTracking() {
        locationTracker.stopLocationUpdates()
    }

    fun autoGenerateIfNeeded() {
        viewModelScope.launch {
            val active = challengeDao.getActiveChallenge()
            if (active == null) {
                if (_currentLocation.value != null) {
                    generateNewChallenge()
                } else {
                    waitingForFirstLocationMission = true
                    _locationStatus.value = "Waiting for the first real GPS fix..."
                    refreshLocation()
                }
            }
        }
    }

    fun generateNewChallenge() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                if (!locationTracker.hasLocationPermission()) {
                    _locationStatus.value = "Location permission is required for dynamic missions."
                    _message.value = "Grant location permission before generating a mission."
                    return@launch
                }

                val generationLocation = locationTracker.getCurrentLocation() ?: _currentLocation.value
                if (generationLocation == null) {
                    _locationStatus.value = "No real device location available. Grant permission and enable GPS."
                    _message.value = "Cannot generate a dynamic mission without your real location."
                    return@launch
                }
                if (!locationTracker.isUsableLocation(generationLocation)) {
                    _currentLocation.value = null
                    _locationStatus.value = locationTracker.explainRejectedLocation(generationLocation)
                    _message.value = "Set a real device or emulator location before generating a mission."
                    return@launch
                }
                _currentLocation.value = generationLocation
                _locationStatus.value = formatLocationStatus(generationLocation)

                val history = challengeDao.getRecentChallenges(20)
                val places = withTimeoutOrNull(20_000L) {
                    fetchPlacesWithExpandingRadius(generationLocation)
                }?.toMutableList() ?: mutableListOf()

                if (places.isEmpty()) {
                    _message.value = "No OpenStreetMap places found near your current location. Try again outdoors or with internet."
                    return@launch
                }

                val newChallenge = generator.generateChallenge(
                    generationLocation,
                    places.distinctBy { "${it.name}:${it.lat}:${it.lng}" },
                    history
                )

                if (newChallenge != null) {
                    challengeDao.expireActiveChallenges(System.currentTimeMillis())
                    challengeDao.insert(newChallenge)
                    notificationHelper.showMissionGenerated(newChallenge)
                    _message.value = "New dynamic mission generated from your current location."
                } else {
                    _message.value = "Could not generate a valid mission right now."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Could not generate a mission right now. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkCompletion() {
        viewModelScope.launch {
            val challenge = _activeChallenge.value ?: run {
                _message.value = "No active mission to verify."
                return@launch
            }

            if (!locationTracker.hasLocationPermission()) {
                _locationStatus.value = "Location permission is required to verify completion."
                _message.value = "Grant location permission before checking completion."
                return@launch
            }

            val location = locationTracker.getCurrentLocation() ?: _currentLocation.value
            if (location == null) {
                _locationStatus.value = "Device location unavailable. Turn on location services and try again."
                _message.value = "Could not get current location to verify completion."
                return@launch
            }
            if (!locationTracker.isUsableLocation(location)) {
                _currentLocation.value = null
                _locationStatus.value = locationTracker.explainRejectedLocation(location)
                _message.value = "Set a real device or emulator location before checking completion."
                return@launch
            }
            _currentLocation.value = location
            _locationStatus.value = formatLocationStatus(location)

            val distance = locationTracker.calculateDistance(
                location.latitude,
                location.longitude,
                challenge.targetLat,
                challenge.targetLng
            )

            if (distance <= COMPLETION_RADIUS_METERS) {
                val completedChallenge = challenge.copy(
                    status = "COMPLETED",
                    completedAt = System.currentTimeMillis()
                )
                challengeDao.update(completedChallenge)
                notificationHelper.showMissionCompleted(completedChallenge)
                _message.value = "Mission completed within ${COMPLETION_RADIUS_METERS} m. Great job!"
            } else {
                val remaining = (distance - COMPLETION_RADIUS_METERS).coerceAtLeast(0f).toInt()
                _message.value = "You are ${distance.toInt()} m from the target. Move about $remaining m closer to complete it."
            }
        }
    }

    fun refreshLocation(showMessage: Boolean = false) {
        viewModelScope.launch {
            if (!locationTracker.hasLocationPermission()) {
                _locationStatus.value = "Location permission is required for dynamic missions."
                if (showMessage) {
                    _message.value = "Grant location permission first."
                }
                return@launch
            }

            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                if (locationTracker.isUsableLocation(location)) {
                    _currentLocation.value = location
                    _locationStatus.value = formatLocationStatus(location)
                    if (showMessage) {
                        _message.value = "Map moved to your current location."
                    }
                } else {
                    _currentLocation.value = null
                    _locationStatus.value = locationTracker.explainRejectedLocation(location)
                    if (showMessage) {
                        _message.value = "Set a real device or emulator location first."
                    }
                }
            } else {
                _locationStatus.value = "Device location unavailable. Enable GPS or set an emulator location."
                if (showMessage) {
                    _message.value = "Could not get current location. Check GPS and permissions."
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationUpdates()
    }

    private suspend fun fetchPlacesWithExpandingRadius(location: Location): List<Place> {
        searchRadiiMeters.forEach { radius ->
            val places = fetchPlaces(location, radius)
            if (places.isNotEmpty()) {
                return places
            }
        }
        return emptyList()
    }

    private suspend fun fetchPlaces(location: Location, radius: Int): List<Place> {
        return coroutineScope {
            challengeCategories.map { category ->
                async {
                    repository.getNearbyPlaces(
                        location.latitude,
                        location.longitude,
                        radius,
                        category
                    )
                }
            }.awaitAll().flatten()
        }
    }

    private fun formatLocationStatus(location: Location): String {
        return String.format(
            Locale.US,
            "Current location: %.5f, %.5f",
            location.latitude,
            location.longitude
        )
    }
}
