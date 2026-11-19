package com.example

import com.example.model.AiMealScanResponse
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.SavedRecipe
import com.example.model.SavedRecipeItem
import com.example.model.ScannedFoodItem
import com.example.model.TotalNutritionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedRecipeUnitTest {

    @Test
    fun withRecalculatedTotals_sumsAllItemsAccurately() {
        val recipe = SavedRecipe(
            userId = "user_1",
            title = "Dal Bhat & Veg",
            category = SavedRecipe.CATEGORY_SCANNED_COMBO,
            mealType = "Lunch",
            items = listOf(
                SavedRecipeItem(
                    foodName = "Steamed Rice",
                    quantityG = 180.0,
                    caloriesKcal = 234,
                    proteinG = 4.32,
                    carbohydratesG = 50.4,
                    fatG = 0.54,
                    fiberG = 0.72,
                    mealSlot = "Lunch"
                ),
                SavedRecipeItem(
                    foodName = "Yellow Dal",
                    quantityG = 200.0,
                    caloriesKcal = 216,
                    proteinG = 13.84,
                    carbohydratesG = 31.2,
                    fatG = 3.81,
                    fiberG = 5.44,
                    mealSlot = "Lunch"
                )
            )
        ).withRecalculatedTotals()

        assertEquals(450, recipe.totalCalories)
        assertEquals(18.2, recipe.totalProteinG, 0.01)
        assertEquals(81.6, recipe.totalCarbsG, 0.01)
        assertEquals(4.4, recipe.totalFatG, 0.01)
        assertEquals(6.2, recipe.totalFiberG, 0.01)
        assertEquals(380.0, recipe.totalWeightG, 0.01)
    }

    @Test
    fun fromScannedMeal_preservesScannedItemsAndUsesScannedComboCategory() {
        val scan = AiMealScanResponse(
            status = "success",
            mealName = "Masala Dosa & Filter Coffee",
            foodItems = listOf(
                ScannedFoodItem("Masala Dosa", 220.0, 380, 9.0, 56.0, 12.0, 4.5, 2.0, 0.94, "South Indian"),
                ScannedFoodItem("Coconut Chutney", 50.0, 110, 2.0, 4.0, 9.5, 2.0, 1.0, 0.90, "Fresh coconut")
            ),
            totalNutrition = TotalNutritionSummary(270.0, 490, 11.0, 60.0, 21.5, 6.5, 3.0),
            analysisNotes = "Crispy dosa with chutney",
            requiresUserConfirmation = true
        )

        val saved = SavedRecipe.fromScannedMeal(
            userId = "user_1",
            customTitle = "Morning Dosa Combo",
            scan = scan,
            mealType = "Breakfast"
        )

        assertEquals("Morning Dosa Combo", saved.title)
        assertEquals(SavedRecipe.CATEGORY_SCANNED_COMBO, saved.category)
        assertEquals("Breakfast", saved.mealType)
        assertEquals(490, saved.totalCalories)
        assertEquals(2, saved.items.size)
        assertTrue(saved.isFavorite)
    }

    @Test
    fun fromMealPlanAndToFoodDiaryEntries_preservesSlotsAndValidFirestoreSource() {
        val plan = MealPlanEntry(
            userId = "user_1",
            title = "High-Protein Indian Day",
            dietType = "High-Protein Indian",
            targetCalories = 2000,
            breakfastSummary = "Moong Dal Chilla",
            lunchSummary = "Paneer Bhurji & Roti",
            dinnerSummary = "Grilled Chicken & Salad",
            snacksSummary = "Roasted Chana"
        )

        val savedPlan = SavedRecipe.fromMealPlan("user_1", plan)
        assertEquals(SavedRecipe.CATEGORY_CUSTOM_MEAL_PLAN, savedPlan.category)
        assertEquals("Full Day", savedPlan.mealType)
        assertEquals(4, savedPlan.items.size)

        val fullDayEntries = savedPlan.toFoodDiaryEntries(dateKey = "2026-11-14")
        assertEquals(listOf("Breakfast", "Lunch", "Dinner", "Snacks"), fullDayEntries.map { it.mealType })
        assertTrue(fullDayEntries.all { it.source == "Manual" && it.dateKey == "2026-11-14" })

        val dinnerEntries = savedPlan.toFoodDiaryEntries(dateKey = "2026-11-14", targetMealTypeOverride = "Dinner")
        assertTrue(dinnerEntries.all { it.mealType == "Dinner" })
    }

    @Test
    fun fromDiaryEntries_buildsFavoriteMealFromLoggedDishes() {
        val entries = listOf(
            FoodDiaryEntry(
                foodName = "Steamed Momo",
                quantity = 180.0,
                calories = 310,
                protein = 18.0,
                carbohydrates = 38.0,
                fat = 8.5,
                fiber = 2.5,
                mealType = "Snacks"
            )
        )
        val saved = SavedRecipe.fromDiaryEntries(
            userId = "user_1",
            title = "Evening Momo Treat",
            mealType = "Snacks",
            entries = entries
        )

        assertEquals("Evening Momo Treat", saved.title)
        assertEquals(SavedRecipe.CATEGORY_FAVORITE_MEAL, saved.category)
        assertEquals(310, saved.totalCalories)
        assertEquals(180.0, saved.totalWeightG, 0.01)
    }
}
