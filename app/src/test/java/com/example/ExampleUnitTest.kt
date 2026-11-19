package com.example

import com.example.data.FoodDatabaseCatalog
import com.example.model.AiMealScanResponse
import com.example.model.ScannedFoodItem
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun mifflinStJeor_calculatesReasonableCalorieTarget() {
        val targetLose = UserProfile.calculateDailyCalorieTarget(
            age = 28,
            heightCm = 168.0,
            weightKg = 70.0,
            gender = "Male",
            activityLevel = "Moderately Active",
            fitnessGoal = "Lose Weight"
        )
        val targetGain = UserProfile.calculateDailyCalorieTarget(
            age = 28,
            heightCm = 168.0,
            weightKg = 70.0,
            gender = "Male",
            activityLevel = "Moderately Active",
            fitnessGoal = "Gain Weight"
        )
        assertTrue(targetLose in 1500..2500)
        assertTrue(targetGain > targetLose)
    }

    @Test
    fun recalculateTotals_sumsScannedFoodItemsAccurately() {
        val items = listOf(
            ScannedFoodItem(
                foodName = "Cooked Rice",
                quantityG = 150.0,
                caloriesKcal = 195,
                proteinG = 3.6,
                carbohydratesG = 42.3,
                fatG = 0.4,
                fiberG = 0.6,
                sugarG = 0.1,
                confidence = 0.92,
                assumptions = "1 bowl steamed rice"
            ),
            ScannedFoodItem(
                foodName = "Rohu Fish Curry (Macher Jhol)",
                quantityG = 180.0,
                caloriesKcal = 235,
                proteinG = 22.4,
                carbohydratesG = 7.8,
                fatG = 12.5,
                fiberG = 1.6,
                sugarG = 1.8,
                confidence = 0.89,
                assumptions = "1 piece fish with light gravy"
            )
        )
        val totals = AiMealScanResponse.recalculateTotals(items)
        assertEquals(330.0, totals.totalWeightG, 0.01)
        assertEquals(430, totals.caloriesKcal)
        assertEquals(26.0, totals.proteinG, 0.01)
        assertEquals(50.1, totals.carbohydratesG, 0.01)
        assertEquals(12.9, totals.fatG, 0.01)
    }

    @Test
    fun foodDatabaseSearch_findsBengaliAndIndianDishes() {
        val bengaliResults = FoodDatabaseCatalog.search("Macher Jhol", "Bengali Foods")
        assertTrue(bengaliResults.isNotEmpty())
        val riceResults = FoodDatabaseCatalog.search("ভাত", "All")
        assertTrue(riceResults.isNotEmpty())
    }

    @Test
    fun foodDatabaseCatalog_includesIndianSouthIndianAndNepaliRecipes() {
        val indianRecipes = FoodDatabaseCatalog.search("", "Indian Foods", recipesOnly = true)
        assertTrue(indianRecipes.size >= 8)
        assertTrue(indianRecipes.all { it.hasRecipe && it.ingredients.isNotEmpty() && it.recipeSteps.isNotEmpty() })

        val southIndianRecipes = FoodDatabaseCatalog.search("", "South Indian Foods", recipesOnly = true)
        assertTrue(southIndianRecipes.size >= 8)
        assertTrue(southIndianRecipes.any { it.nameEn.contains("Masala Dosa", ignoreCase = true) })

        val nepaliRecipes = FoodDatabaseCatalog.search("", "Nepali Foods", recipesOnly = true)
        assertTrue(nepaliRecipes.size >= 8)
        assertTrue(nepaliRecipes.any { it.nameEn.contains("Dal Bhat", ignoreCase = true) })
        assertTrue(nepaliRecipes.any { it.nameEn.contains("Momo", ignoreCase = true) })
    }
}
