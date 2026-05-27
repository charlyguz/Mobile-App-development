package com.example.carlosguzmandatapersistence

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.room.Room

class FlowerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = buildFlowerDatabase()
        val flowerDAO = getFlowerDAO(db)
        addFlower("Roza", "Rose", "Rosa", flowerDAO)
        printAllFlowers(flowerDAO)
    }

    private fun addFlower(
        polishName: String?,
        englishName: String?,
        spanishName: String?,
        flowerDAO: FlowerDao
    ) {
        val flower = Flower(polishName, englishName, spanishName)
        flowerDAO.insertAll(flower)
    }

    private fun printAllFlowers(flowerDAO: FlowerDao) {
        val flowers = flowerDAO.getAll()
        for (flower in flowers) {
            Log.d(TAG, flower.toString())
        }
    }

    private fun getFlowerDAO(db: FlowerDatabase): FlowerDao {
        return db.flowerDAO()
    }

    private fun buildFlowerDatabase(): FlowerDatabase {
        return Room.databaseBuilder(
            applicationContext,
            FlowerDatabase::class.java,
            "flowers"
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

    companion object {
        private const val TAG = "FlowerActivity"
    }
}
