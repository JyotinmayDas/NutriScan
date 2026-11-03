package com.example.features.weight_tracker

import com.example.model.WeightRecord
import kotlin.math.roundToInt

/**
 * Clean Architecture Feature Module: Weight & BMI Tracker (`features/weight_tracker`)
 */
object WeightTrackerFeature {
    fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        val heightM = (heightCm / 100.0).coerceAtLeast(0.5)
        return ((weightKg / (heightM * heightM)) * 10.0).roundToInt() / 10.0
    }

    fun calculateBmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    fun latestWeightOrDefault(history: List<WeightRecord>, fallbackKg: Double): Double =
        history.firstOrNull()?.weight ?: fallbackKg
}
