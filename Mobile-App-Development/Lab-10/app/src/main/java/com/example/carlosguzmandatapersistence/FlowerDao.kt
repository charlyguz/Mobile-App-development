package com.example.carlosguzmandatapersistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FlowerDao {

    @Insert
    fun insertAll(vararg flowers: Flower)

    @Update
    fun update(flower: Flower)

    @Delete
    fun delete(flower: Flower)

    @Query("SELECT * FROM flower_table")
    fun getAll(): List<Flower>
}
