package com.example.cityexplorerchallenge.domain

import android.location.Location
import com.example.cityexplorerchallenge.data.local.ChallengeEntity
import com.example.cityexplorerchallenge.data.remote.Place
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

class ChallengeGenerator(
    private val currentHourProvider: () -> Int = {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
) {

    fun generateChallenge(
        currentLocation: Location,
        nearbyPlaces: List<Place>,
        history: List<ChallengeEntity>
    ): ChallengeEntity? {
        return generateChallenge(
            currentLat = currentLocation.latitude,
            currentLng = currentLocation.longitude,
            nearbyPlaces = nearbyPlaces,
            history = history
        )
    }

    fun generateChallenge(
        currentLat: Double,
        currentLng: Double,
        nearbyPlaces: List<Place>,
        history: List<ChallengeEntity>
    ): ChallengeEntity? {
        if (nearbyPlaces.isEmpty()) return null

        var validPlaces = nearbyPlaces.toMutableList()
        val reasons = mutableListOf<String>()

        // Rule 1: avoid repeating recently suggested or completed places.
        val recentlyVisitedNames = history
            .take(10)
            .map { it.targetPlaceName.ifEmpty { it.title } }
            .toSet()
        val countBeforeRepeatFilter = validPlaces.size
        validPlaces.removeAll { recentlyVisitedNames.contains(it.name) }
        if (validPlaces.size < countBeforeRepeatFilter) {
            reasons.add("Recently visited places were excluded")
        }

        if (validPlaces.isEmpty()) {
            validPlaces = nearbyPlaces.toMutableList()
            reasons.add("All nearby places were recently used, so the best available option was restored")
        }

        // Rule 2: if the last three challenges used one category, force variety.
        val recentCategories = history.take(3).map { it.category }
        val allSameRecentCategory = recentCategories.size == 3 && recentCategories.distinct().size == 1
        var forcedCategory: String? = null

        if (allSameRecentCategory) {
            val overused = recentCategories.first()
            val differentCategories = validPlaces
                .map { it.category }
                .distinct()
                .filter { it != overused }

            if (differentCategories.isNotEmpty()) {
                forcedCategory = differentCategories.first()
                reasons.add("You have explored $overused several times, so this mission changes category to $forcedCategory")
                validPlaces.retainAll { it.category == forcedCategory }
            }
        }

        if (validPlaces.isEmpty()) {
            validPlaces = nearbyPlaces.toMutableList()
        }

        // Rule 3: adapt category preference to time of day.
        val hour = currentHourProvider()

        if (forcedCategory == null) {
            when (hour) {
                in 6..11 -> {
                    val foodPlaces = validPlaces.filter { it.category == "Food" }
                    if (foodPlaces.isNotEmpty()) {
                        validPlaces = foodPlaces.toMutableList()
                        reasons.add("Morning hours favor cafes and restaurants")
                    }
                }
                in 12..17 -> {
                    val outdoorPlaces = validPlaces.filter { it.category == "Nature" || it.category == "Culture" }
                    if (outdoorPlaces.isNotEmpty()) {
                        validPlaces = outdoorPlaces.toMutableList()
                        reasons.add("Afternoon is a good time for culture or nature")
                    }
                }
                in 18..22 -> {
                    val eveningPlaces = validPlaces.filter { it.category == "Food" || it.category == "Sport" }
                    if (eveningPlaces.isNotEmpty()) {
                        validPlaces = eveningPlaces.toMutableList()
                        reasons.add("Evening hours favor food or sport locations")
                    }
                }
            }
        }

        // Rule 4 and 5: experienced users get slightly farther missions; new users get closer ones.
        val totalCompleted = history.count { it.status == "COMPLETED" }
        val experienced = totalCompleted > 5

        validPlaces.sortBy { place ->
            distanceBetween(currentLat, currentLng, place)
        }

        val selectedPlace = if (experienced && validPlaces.size > 2) {
            reasons.add("You have completed $totalCompleted missions, so this target is a bit farther")
            validPlaces[validPlaces.size / 2]
        } else {
            reasons.add("A nearby place was selected to make the mission reachable")
            validPlaces.first()
        }

        val distance = distanceBetween(currentLat, currentLng, selectedPlace).roundToInt()

        val title = when (selectedPlace.category) {
            "Nature" -> "Visit a park or nature spot nearby"
            "Culture" -> "Explore a historical landmark"
            "Food" -> "Find a restaurant or cafe"
            "Sport" -> "Discover a sports venue"
            else -> "Explore a new place"
        }

        return ChallengeEntity(
            title = title,
            description = "Head to ${selectedPlace.name} and discover something new in your city.",
            category = selectedPlace.category,
            targetLat = selectedPlace.lat,
            targetLng = selectedPlace.lng,
            distanceMeters = distance,
            status = "ACTIVE",
            reason = reasons.joinToString("\n") { "- $it" },
            targetPlaceName = selectedPlace.name
        )
    }

    private fun distanceBetween(currentLat: Double, currentLng: Double, place: Place): Float {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(currentLat)
        val lat2 = Math.toRadians(place.lat)
        val deltaLat = Math.toRadians(place.lat - currentLat)
        val deltaLng = Math.toRadians(place.lng - currentLng)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).toFloat()
    }
}
