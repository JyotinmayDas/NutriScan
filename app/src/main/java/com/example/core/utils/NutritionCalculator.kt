package com.example.core.utils

import com.example.model.AiMealScanResponse
import com.example.model.ScannedFoodItem
import com.example.model.TotalNutritionSummary
import com.example.model.UserProfile
import kotlin.math.roundToInt

object NutritionCalculator {
    /**
     * Estimates daily calorie target using the Mifflin-St Jeor formula.
     * Note: Always accompanied by an educational estimate disclaimer.
     */
    fun calculateDailyCalorieGoal(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        gender: String,
        activityLevel: String,
        fitnessGoal: String
    ): Int = UserProfile.calculateDailyCalorieTarget(
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        gender = gender,
        activityLevel = activityLevel,
        fitnessGoal = fitnessGoal
    )

    /**
     * Calculates recommended daily macronutrient targets in grams (25% Protein, 50% Carbs, 25% Fat).
     */
    fun calculateMacroTargets(dailyCalorieGoal: Int): Triple<Int, Int, Int> {
        val safeCalories = dailyCalorieGoal.coerceIn(1000, 6000)
        val proteinG = ((safeCalories * 0.25) / 4.0).roundToInt().coerceAtLeast(50)
        val carbsG = ((safeCalories * 0.50) / 4.0).roundToInt().coerceAtLeast(120)
        val fatG = ((safeCalories * 0.25) / 9.0).roundToInt().coerceAtLeast(35)
        return Triple(proteinG, carbsG, fatG)
    }

    /**
     * Dynamically computes total meal nutrition strictly from individual food items.
     */
    fun calculateMealTotals(items: List<ScannedFoodItem>): TotalNutritionSummary {
        return AiMealScanResponse.recalculateTotals(items)
    }
}
