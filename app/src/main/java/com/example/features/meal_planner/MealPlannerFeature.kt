package com.example.features.meal_planner

import com.example.model.MealPlanEntry

/**
 * Clean Architecture Feature Module: Personalized Meal Planner (`features/meal_planner`)
 */
object MealPlannerFeature {
    val supportedDietaryStyles = listOf(
        "Bengali Traditional",
        "Indian Vegetarian",
        "Non-Vegetarian",
        "Vegan",
        "High-Protein Indian"
    )

    fun formatPlanSummary(plan: MealPlanEntry): String =
        "${plan.title} (${plan.targetCalories} kcal/day)"
}
