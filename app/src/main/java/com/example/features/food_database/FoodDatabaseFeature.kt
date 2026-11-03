package com.example.features.food_database

import com.example.data.FoodDatabaseCatalog
import com.example.model.FoodDatabaseItem

/**
 * Clean Architecture Feature Module: Indian, Bengali & Global Food Database (`features/food_database`)
 */
object FoodDatabaseFeature {
    fun searchFoods(query: String, category: String = "All"): List<FoodDatabaseItem> =
        FoodDatabaseCatalog.search(query, category)

    fun availableCategories(): List<String> = FoodDatabaseCatalog.categories
}
