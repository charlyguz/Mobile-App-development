package com.example.carlosguzmandatapersistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Flower::class], version = 2, exportSchema = false)
abstract class FlowerDatabase : RoomDatabase() {
    abstract fun flowerDAO(): FlowerDao
}
