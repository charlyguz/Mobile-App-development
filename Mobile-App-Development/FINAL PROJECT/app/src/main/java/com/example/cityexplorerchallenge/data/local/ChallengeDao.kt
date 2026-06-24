package com.example.cityexplorerchallenge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Insert
    suspend fun insert(challenge: ChallengeEntity): Long

    @Update
    suspend fun update(challenge: ChallengeEntity)

    @Query("SELECT * FROM challenges WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveChallenge(): ChallengeEntity?

    @Query("SELECT * FROM challenges WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveChallengeFlow(): Flow<ChallengeEntity?>

    @Query("UPDATE challenges SET status = 'EXPIRED', expiredAt = :expiredAt WHERE status = 'ACTIVE'")
    suspend fun expireActiveChallenges(expiredAt: Long)

    @Query("SELECT * FROM challenges WHERE status != 'ACTIVE' ORDER BY generatedAt DESC")
    fun getHistoryFlow(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun getAllChallenges(): List<ChallengeEntity>
    
    @Query("SELECT * FROM challenges ORDER BY generatedAt DESC LIMIT :limit")
    suspend fun getRecentChallenges(limit: Int): List<ChallengeEntity>
}
