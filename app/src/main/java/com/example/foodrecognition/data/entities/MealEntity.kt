package com.example.foodrecognition.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val imagePath: String?,
    val totalCalories: Int,
    val itemsJson: String // JSON list of food items
)
