package com.example

import com.example.model.AiMealScanResponse
import com.example.model.ScannedFoodItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PortionEstimatorTest {

    private fun sampleMealScan(): AiMealScanResponse {
        val items = listOf(
            ScannedFoodItem(
                foodName = "Steamed Rice",
                quantityG = 150.0,
                caloriesKcal = 195,
                proteinG = 4.0,
                carbohydratesG = 42.0,
                fatG = 0.5,
                fiberG = 0.6,
                sugarG = 0.1,
                confidence = 0.92,
                assumptions = "1 medium katori"
            ),
            ScannedFoodItem(
                foodName = "Dal Tadka",
                quantityG = 200.0,
                caloriesKcal = 240,
                proteinG = 12.0,
                carbohydratesG = 30.0,
                fatG = 8.0,
                fiberG = 6.0,
                sugarG = 2.0,
                confidence = 0.88,
                assumptions = "1 bowl"
            )
        )
        return AiMealScanResponse(
            status = "success",
            mealName = "Dal Rice Combo",
            foodItems = items,
            totalNutrition = AiMealScanResponse.recalculateTotals(items),
            analysisNotes = "Balanced home meal",
            requiresUserConfirmation = true
        )
    }

    @Test
    fun scaledByHalf_halvesCaloriesWeightAndMacros() {
        val base = sampleMealScan()
        val half = base.scaledBy(0.5f)

        assertEquals(175.0, half.totalNutrition.totalWeightG, 0.1)
        assertEquals(218, half.totalNutrition.caloriesKcal) // 98 + 120
        assertEquals(8.0, half.totalNutrition.proteinG, 0.1)
        assertEquals(36.0, half.totalNutrition.carbohydratesG, 0.1)
        assertEquals(4.3, half.totalNutrition.fatG, 0.1) // 0.3 + 4.0
    }

    @Test
    fun scaledByDouble_doublesCaloriesWeightAndMacros() {
        val base = sampleMealScan()
        val doubled = base.scaledBy(2.0f)

        assertEquals(700.0, doubled.totalNutrition.totalWeightG, 0.1)
        assertEquals(870, doubled.totalNutrition.caloriesKcal)
        assertEquals(32.0, doubled.totalNutrition.proteinG, 0.1)
        assertEquals(144.0, doubled.totalNutrition.carbohydratesG, 0.1)
        assertEquals(17.0, doubled.totalNutrition.fatG, 0.1)
    }

    @Test
    fun scaledByFromBase_preservesExactValuesWhenReturningToOne() {
        val base = sampleMealScan()
        // Simulates sliding 1.0x -> 0.5x -> 2.0x -> 1.0x using the lossless baseResult
        base.scaledBy(0.5f)
        base.scaledBy(2.0f)
        val restored = base.scaledBy(1.0f)

        assertEquals(base.totalNutrition.totalWeightG, restored.totalNutrition.totalWeightG, 0.01)
        assertEquals(base.totalNutrition.caloriesKcal, restored.totalNutrition.caloriesKcal)
        assertEquals(base.totalNutrition.proteinG, restored.totalNutrition.proteinG, 0.01)
        assertEquals(base.totalNutrition.carbohydratesG, restored.totalNutrition.carbohydratesG, 0.01)
        assertEquals(base.totalNutrition.fatG, restored.totalNutrition.fatG, 0.01)
    }
}
