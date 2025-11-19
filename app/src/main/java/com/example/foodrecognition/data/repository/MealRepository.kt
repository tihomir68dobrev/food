package com.example.foodrecognition.data.repository

import com.example.foodrecognition.data.entities.MealEntity
import com.example.foodrecognition.data.database.MealDao


class MealRepository(private val dao: MealDao) {

    suspend fun saveMeal(meal: MealEntity) {
        dao.insertMeal(meal)
    }

    fun getHistory() = dao.getAllMeals()

    suspend fun getMeal(mealId: Long) = dao.getMealById(mealId)
}
