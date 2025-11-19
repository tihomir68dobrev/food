package com.example.foodrecognition.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodrecognition.data.entities.MealEntity

@Database(entities = [MealEntity::class], version = 1)
abstract class MealDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile private var INSTANCE: MealDatabase? = null

        fun getDatabase(context: Context): MealDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MealDatabase::class.java,
                    "meal_database"
                ).build().also { INSTANCE = it }
            }
    }
}
