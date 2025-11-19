package com.example.foodrecognition.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodrecognition.data.database.MealDatabase
import com.example.foodrecognition.data.entities.MealEntity
import com.example.foodrecognition.data.repository.MealRepository
import kotlinx.coroutines.launch

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MealDatabase.getDatabase(application).mealDao()
    private val repository = MealRepository(dao)

    val history = repository.getHistory()   // Flow<List<MealEntity>>

    fun saveMeal(meal: MealEntity) {
        viewModelScope.launch {
            repository.saveMeal(meal)
        }
    }
}


