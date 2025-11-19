package com.example.foodrecognition.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foodrecognition.data.entities.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealById(mealId: Long): MealEntity
}

