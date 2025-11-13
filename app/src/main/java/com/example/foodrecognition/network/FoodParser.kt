package com.example.foodrecognition.network

data class FoodItem(val name: String, val calories: String)

fun parseFoodResponse(responseText: String): List<FoodItem> {
    val lines = responseText.split("\n")
    val foodItems = mutableListOf<FoodItem>()

    for (line in lines) {
        val cleanLine = line
            .replace("*", "")
            .replace("-", "—")
            .trim()

        if (cleanLine.contains("—")) {
            val parts = cleanLine.split("—", limit = 2)
            val food = parts[0].trim()
            val cal = parts.getOrNull(1)?.trim() ?: ""
            if (food.isNotEmpty()) {
                foodItems.add(FoodItem(food, cal))
            }
        }
    }
    return foodItems
}
