package com.example.cityexplorerchallenge.domain

import com.example.cityexplorerchallenge.data.local.ChallengeEntity
import com.example.cityexplorerchallenge.data.remote.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeGeneratorTest {

    @Test
    fun returns_null_when_no_places_are_available() {
        val generator = ChallengeGenerator(currentHourProvider = { 23 })

        val result = generator.generateChallenge(
            currentLat = BASE_LAT,
            currentLng = BASE_LNG,
            nearbyPlaces = emptyList(),
            history = emptyList()
        )

        assertNull(result)
    }

    @Test
    fun excludes_recent_places_before_selecting_nearest_target() {
        val generator = ChallengeGenerator(currentHourProvider = { 23 })
        val recent = place(1, "Recent Cafe", 0.001, "Food")
        val nextNearest = place(2, "New Park", 0.002, "Nature")

        val challenge = requireChallenge(
            generator.generateChallenge(
                currentLat = BASE_LAT,
                currentLng = BASE_LNG,
                nearbyPlaces = listOf(recent, nextNearest),
                history = listOf(historyItem(targetPlaceName = recent.name, category = "Food"))
            )
        )

        assertEquals("New Park", challenge.targetPlaceName)
        assertTrue(challenge.reason.contains("Recently visited places were excluded"))
    }

    @Test
    fun morning_prefers_food_when_food_places_are_available() {
        val generator = ChallengeGenerator(currentHourProvider = { 8 })

        val challenge = requireChallenge(
            generator.generateChallenge(
                currentLat = BASE_LAT,
                currentLng = BASE_LNG,
                nearbyPlaces = listOf(
                    place(1, "Nearby Park", 0.001, "Nature"),
                    place(2, "Breakfast Cafe", 0.002, "Food")
                ),
                history = emptyList()
            )
        )

        assertEquals("Food", challenge.category)
        assertEquals("Breakfast Cafe", challenge.targetPlaceName)
    }

    @Test
    fun repeated_recent_category_forces_a_different_category() {
        val generator = ChallengeGenerator(currentHourProvider = { 8 })
        val foodHistory = listOf(
            historyItem(targetPlaceName = "Old Cafe 1", category = "Food"),
            historyItem(targetPlaceName = "Old Cafe 2", category = "Food"),
            historyItem(targetPlaceName = "Old Cafe 3", category = "Food")
        )

        val challenge = requireChallenge(
            generator.generateChallenge(
                currentLat = BASE_LAT,
                currentLng = BASE_LNG,
                nearbyPlaces = listOf(
                    place(1, "Another Cafe", 0.001, "Food"),
                    place(2, "City Park", 0.002, "Nature"),
                    place(3, "Small Museum", 0.003, "Culture")
                ),
                history = foodHistory
            )
        )

        assertEquals("Nature", challenge.category)
        assertEquals("City Park", challenge.targetPlaceName)
        assertTrue(challenge.reason.contains("changes category to Nature"))
    }

    @Test
    fun experienced_users_receive_a_farther_mid_distance_target() {
        val generator = ChallengeGenerator(currentHourProvider = { 23 })
        val completedHistory = (1..6).map {
            historyItem(targetPlaceName = "Completed $it", category = "Sport")
        }

        val challenge = requireChallenge(
            generator.generateChallenge(
                currentLat = BASE_LAT,
                currentLng = BASE_LNG,
                nearbyPlaces = listOf(
                    place(1, "Near Sport", 0.001, "Sport"),
                    place(2, "Middle Sport", 0.002, "Sport"),
                    place(3, "Far Sport", 0.003, "Sport"),
                    place(4, "Farthest Sport", 0.004, "Sport")
                ),
                history = completedHistory
            )
        )

        assertEquals("Far Sport", challenge.targetPlaceName)
        assertTrue(challenge.reason.contains("this target is a bit farther"))
    }

    @Test
    fun new_users_receive_the_nearest_target() {
        val generator = ChallengeGenerator(currentHourProvider = { 23 })

        val challenge = requireChallenge(
            generator.generateChallenge(
                currentLat = BASE_LAT,
                currentLng = BASE_LNG,
                nearbyPlaces = listOf(
                    place(1, "Far Museum", 0.003, "Culture"),
                    place(2, "Near Museum", 0.001, "Culture"),
                    place(3, "Middle Museum", 0.002, "Culture")
                ),
                history = emptyList()
            )
        )

        assertEquals("Near Museum", challenge.targetPlaceName)
        assertTrue(challenge.reason.contains("A nearby place was selected"))
    }

    private fun requireChallenge(challenge: ChallengeEntity?): ChallengeEntity {
        return challenge ?: error("Expected a challenge to be generated")
    }

    private fun place(id: Long, name: String, latOffset: Double, category: String): Place {
        return Place(
            id = id,
            name = name,
            lat = BASE_LAT + latOffset,
            lng = BASE_LNG,
            category = category
        )
    }

    private fun historyItem(targetPlaceName: String, category: String): ChallengeEntity {
        return ChallengeEntity(
            title = "Visit $targetPlaceName",
            description = "Completed test mission",
            category = category,
            targetLat = BASE_LAT,
            targetLng = BASE_LNG,
            distanceMeters = 100,
            status = "COMPLETED",
            reason = "Test history",
            targetPlaceName = targetPlaceName,
            completedAt = 1L
        )
    }

    private companion object {
        const val BASE_LAT = 50.0614
        const val BASE_LNG = 19.9366
    }
}
