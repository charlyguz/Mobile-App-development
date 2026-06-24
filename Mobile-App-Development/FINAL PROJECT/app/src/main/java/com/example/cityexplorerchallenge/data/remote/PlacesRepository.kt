package com.example.cityexplorerchallenge.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class PlacesRepository {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "CityExplorerChallenge/1.0 (Android final project)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/api/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OverpassApi::class.java)

    suspend fun getNearbyPlaces(lat: Double, lng: Double, radius: Int = 1000, category: String): List<Place> {
        val query = buildNearbyPlacesQuery(lat, lng, radius, category)

        return try {
            val response = api.getNearbyPlaces(query)
            response.elements.mapNotNull { element ->
                val name = element.tags?.get("name")
                val placeLat = element.lat ?: element.center?.lat
                val placeLng = element.lon ?: element.center?.lon

                if (name != null && placeLat != null && placeLng != null) {
                    Place(
                        id = element.id,
                        name = name,
                        lat = placeLat,
                        lng = placeLng,
                        category = category
                    )
                } else null
            }.distinctBy { "${it.name}:${it.lat}:${it.lng}" }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun buildNearbyPlacesQuery(lat: Double, lng: Double, radius: Int, category: String): String {
        val tagQueries = when (category) {
            "Nature" -> listOf(
                "[\"leisure\"~\"park|garden|nature_reserve\"]",
                "[\"boundary\"=\"protected_area\"]"
            )
            "Culture" -> listOf(
                "[\"historic\"~\"monument|memorial|ruins|castle|archaeological_site\"]",
                "[\"tourism\"~\"museum|attraction|artwork\"]"
            )
            "Food" -> listOf("[\"amenity\"~\"restaurant|cafe|fast_food|bar\"]")
            "Sport" -> listOf("[\"leisure\"~\"sports_centre|pitch|stadium|swimming_pool\"]")
            else -> listOf("[\"amenity\"~\"restaurant\"]")
        }

        return buildString {
            append("[out:json][timeout:25];(")
            tagQueries.forEach { tagQuery ->
                append("node(around:$radius,$lat,$lng)$tagQuery;")
                append("way(around:$radius,$lat,$lng)$tagQuery;")
                append("relation(around:$radius,$lat,$lng)$tagQuery;")
            }
            append(");out center 30;")
        }
    }
}
