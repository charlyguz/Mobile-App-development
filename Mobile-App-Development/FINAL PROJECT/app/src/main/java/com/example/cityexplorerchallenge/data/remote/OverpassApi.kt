package com.example.cityexplorerchallenge.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("interpreter")
    suspend fun getNearbyPlaces(
        @Query("data") query: String
    ): OverpassResponse
}
