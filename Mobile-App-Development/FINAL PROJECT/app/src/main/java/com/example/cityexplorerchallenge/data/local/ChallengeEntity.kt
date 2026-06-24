package com.example.cityexplorerchallenge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val targetLat: Double,
    val targetLng: Double,
    val distanceMeters: Int,
    val status: String, // "ACTIVE", "COMPLETED", "EXPIRED"
    val reason: String,
    val targetPlaceName: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val expiredAt: Long? = null
)
