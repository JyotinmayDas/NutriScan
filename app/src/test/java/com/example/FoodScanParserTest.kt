package com.example

import com.example.viewmodel.NutriScanViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FoodScanParserTest {

    @Test
    fun parseAndValidateScanJson_handlesFencedJsonAndStringNumbersSafely() {
        val raw = """
            ```json
            {
              "meal_name": "Dal & Roti",
              "food_items": [
                {
                  "food_name": "Whole Wheat Roti",
                  "quantity_g": "80g",
                  "calories_kcal": 210,
                  "protein_g": 6.5,
                  "carbohydrates_g": 40.0,
                  "fat_g": null
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = NutriScanViewModel.parseAndValidateScanJson(raw)
        assertEquals("Dal & Roti", parsed.mealName)
        assertEquals(1, parsed.foodItems.size)
        assertEquals("Whole Wheat Roti", parsed.foodItems.first().foodName)
        assertEquals(210, parsed.totalNutrition.caloriesKcal)
        assertEquals(80.0, parsed.totalNutrition.totalWeightG, 0.01)
        assertEquals(6.5, parsed.totalNutrition.proteinG, 0.01)
        assertEquals(40.0, parsed.totalNutrition.carbohydratesG, 0.01)
        assertEquals(0.0, parsed.totalNutrition.fatG, 0.01)
    }

    @Test
    fun parseAndValidateScanJson_synthesizesFallbackItemWhenItemsArrayEmpty() {
        val raw = """
            {
              "meal_name": "Chicken Biryani",
              "food_items": [],
              "total_nutrition": {
                "calories_kcal": 540,
                "protein_g": 28.0,
                "carbohydrates_g": 62.0,
                "fat_g": 18.0,
                "total_weight_g": 320.0
              }
            }
        """.trimIndent()

        val parsed = NutriScanViewModel.parseAndValidateScanJson(raw)
        assertEquals("Chicken Biryani", parsed.mealName)
        assertEquals(1, parsed.foodItems.size)
        assertEquals(540, parsed.totalNutrition.caloriesKcal)
        assertEquals(320.0, parsed.totalNutrition.totalWeightG, 0.01)
    }

    @Test
    fun parseAndValidateScanJson_throwsHelpfulMessageWhenNoFoodDetected() {
        val raw = """{"meal_name": "Unknown", "food_items": []}"""
        val error = runCatching { NutriScanViewModel.parseAndValidateScanJson(raw) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("No food detected"))
    }
}
