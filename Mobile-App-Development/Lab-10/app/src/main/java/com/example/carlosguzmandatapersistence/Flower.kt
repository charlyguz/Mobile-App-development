package com.example.carlosguzmandatapersistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flower_table")
data class Flower(
    @ColumnInfo(name = "polish_name")
    val polishName: String?,
    @ColumnInfo(name = "english_name")
    val englishName: String?,
    @ColumnInfo(name = "spanish_name")
    val spanishName: String?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "flower_id")
    val flowerID: Int = 0
)
