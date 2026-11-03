package com.example.features.reports

import com.example.model.FoodDiaryEntry
import kotlin.math.roundToInt

/**
 * Clean Architecture Feature Module: Nutrition & Macro Reports (`features/reports`)
 */
data class MacroDistributionReport(
    val totalCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int
)

object NutritionReportsFeature {
    fun buildDailyMacroReport(entries: List<FoodDiaryEntry>): MacroDistributionReport =
        MacroDistributionReport(
            totalCalories = entries.sumOf { it.calories },
            proteinGrams = entries.sumOf { it.protein }.roundToInt(),
            carbsGrams = entries.sumOf { it.carbohydrates }.roundToInt(),
            fatGrams = entries.sumOf { it.fat }.roundToInt(),
            fiberGrams = entries.sumOf { it.fiber }.roundToInt()
        )
}
