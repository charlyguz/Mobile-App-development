package com.example.cityexplorerchallenge.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

data class Place(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: String
)
