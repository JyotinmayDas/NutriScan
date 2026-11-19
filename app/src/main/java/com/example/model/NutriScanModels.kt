package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun todayDateKey(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "NutriScan Explorer",
    val email: String = "",
    val age: Int = 28,
    val height: Double = 168.0,
    val currentWeight: Double = 68.0,
    val targetWeight: Double = 62.0,
    val gender: String = "Prefer not to say",
    val activityLevel: String = "Moderately Active",
    val fitnessGoal: String = "Lose Weight",
    val dietaryPreference: String = "Bengali Traditional",
    val dailyCalorieGoal: Int = 1850,
    val dailyProteinGoalG: Int = 116,
    val dailyCarbsGoalG: Int = 231,
    val dailyFatGoalG: Int = 51,
    val dailyFiberGoalG: Int = 30,
    val useCustomNutritionGoals: Boolean = false,
    val dailyWaterGoalMl: Int = 2500,
    val language: String = "en",
    val isPremium: Boolean = false,
    val dailyScansUsed: Int = 0,
    val lastScanDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveProteinGoalG(): Int {
        val targetCal = dailyCalorieGoal.coerceAtLeast(800)
        return if (useCustomNutritionGoals && dailyProteinGoalG > 0) {
            dailyProteinGoalG.coerceIn(20, 400)
        } else {
            ((targetCal * 0.25) / 4.0).roundToInt().coerceAtLeast(45)
        }
    }

    fun effectiveCarbsGoalG(): Int {
        val targetCal = dailyCalorieGoal.coerceAtLeast(800)
        return if (useCustomNutritionGoals && dailyCarbsGoalG > 0) {
            dailyCarbsGoalG.coerceIn(30, 700)
        } else {
            ((targetCal * 0.50) / 4.0).roundToInt().coerceAtLeast(90)
        }
    }

    fun effectiveFatGoalG(): Int {
        val targetCal = dailyCalorieGoal.coerceAtLeast(800)
        return if (useCustomNutritionGoals && dailyFatGoalG > 0) {
            dailyFatGoalG.coerceIn(15, 250)
        } else {
            ((targetCal * 0.25) / 9.0).roundToInt().coerceAtLeast(30)
        }
    }

    fun effectiveFiberGoalG(): Int {
        return if (useCustomNutritionGoals && dailyFiberGoalG > 0) {
            dailyFiberGoalG.coerceIn(10, 100)
        } else {
            30
        }
    }

    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "name" to name.take(100).ifBlank { "User" },
            "email" to email.take(150),
            "age" to age.coerceIn(10, 120),
            "height" to height.coerceIn(50.0, 260.0),
            "currentWeight" to currentWeight.coerceIn(15.0, 350.0),
            "targetWeight" to targetWeight.coerceIn(15.0, 350.0),
            "gender" to gender.take(40),
            "activityLevel" to activityLevel.take(40).ifBlank { "Moderately Active" },
            "fitnessGoal" to if (fitnessGoal in listOf("Lose Weight", "Maintain Weight", "Gain Weight")) fitnessGoal else "Maintain Weight",
            "dietaryPreference" to dietaryPreference.take(60).ifBlank { "Vegetarian" },
            "dailyCalorieGoal" to dailyCalorieGoal.coerceIn(800, 6000),
            "dailyWaterGoalMl" to dailyWaterGoalMl.coerceIn(500, 10000),
            "language" to if (language in listOf("en", "bn", "hi")) language else "en",
            "isPremium" to isPremium,
            "dailyScansUsed" to dailyScansUsed.coerceIn(0, 1000),
            "lastScanDate" to lastScanDate.take(20),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "name" to name.take(100).ifBlank { "User" },
            "email" to email.take(150),
            "age" to age.coerceIn(10, 120),
            "height" to height.coerceIn(50.0, 260.0),
            "currentWeight" to currentWeight.coerceIn(15.0, 350.0),
            "targetWeight" to targetWeight.coerceIn(15.0, 350.0),
            "gender" to gender.take(40),
            "activityLevel" to activityLevel.take(40).ifBlank { "Moderately Active" },
            "fitnessGoal" to if (fitnessGoal in listOf("Lose Weight", "Maintain Weight", "Gain Weight")) fitnessGoal else "Maintain Weight",
            "dietaryPreference" to dietaryPreference.take(60).ifBlank { "Vegetarian" },
            "dailyCalorieGoal" to dailyCalorieGoal.coerceIn(800, 6000),
            "dailyWaterGoalMl" to dailyWaterGoalMl.coerceIn(500, 10000),
            "language" to if (language in listOf("en", "bn", "hi")) language else "en",
            "isPremium" to isPremium,
            "dailyScansUsed" to dailyScansUsed.coerceIn(0, 1000),
            "lastScanDate" to lastScanDate.take(20),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        /**
         * Mifflin-St Jeor equation for daily energy expenditure estimate.
         * Note: Presented clearly as an educational estimate, not medical advice.
         */
        fun calculateDailyCalorieTarget(
            age: Int,
            heightCm: Double,
            weightKg: Double,
            gender: String,
            activityLevel: String,
            fitnessGoal: String
        ): Int {
            val genderOffset = when (gender.lowercase(Locale.US)) {
                "male" -> 5.0
                "female" -> -161.0
                else -> -78.0
            }
            val bmr = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + genderOffset
            val multiplier = when (activityLevel) {
                "Sedentary" -> 1.2
                "Lightly Active" -> 1.375
                "Moderately Active" -> 1.55
                "Very Active" -> 1.725
                "Extra Active" -> 1.9
                else -> 1.45
            }
            val tdee = bmr * multiplier
            val goalAdjusted = when (fitnessGoal) {
                "Lose Weight" -> tdee - 400.0
                "Gain Weight" -> tdee + 350.0
                else -> tdee
            }
            return goalAdjusted.roundToInt().coerceIn(1200, 4500)
        }

        /**
         * Calculates Protein (g), Carbs (g), and Fat (g) from a daily calorie target and percentage split.
         */
        fun calculateMacroGramsForSplit(
            calories: Int,
            proteinPct: Int,
            carbsPct: Int,
            fatPct: Int
        ): Triple<Int, Int, Int> {
            val safeCal = calories.coerceIn(800, 6000)
            val pG = ((safeCal * (proteinPct / 100.0)) / 4.0).roundToInt().coerceAtLeast(20)
            val cG = ((safeCal * (carbsPct / 100.0)) / 4.0).roundToInt().coerceAtLeast(30)
            val fG = ((safeCal * (fatPct / 100.0)) / 9.0).roundToInt().coerceAtLeast(15)
            return Triple(pG, cG, fG)
        }
    }
}

@IgnoreExtraProperties
data class FoodDiaryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val foodName: String = "",
    val quantity: Double = 100.0,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val mealType: String = "Lunch",
    val dateKey: String = todayDateKey(),
    val source: String = "Manual",
    val confidence: Double = 1.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "foodName" to foodName.take(120).ifBlank { "Food Item" },
            "quantity" to quantity.coerceIn(1.0, 5000.0),
            "calories" to calories.coerceIn(0, 5000),
            "protein" to protein.coerceIn(0.0, 500.0),
            "carbohydrates" to carbohydrates.coerceIn(0.0, 1000.0),
            "fat" to fat.coerceIn(0.0, 500.0),
            "fiber" to fiber.coerceIn(0.0, 200.0),
            "sugar" to sugar.coerceIn(0.0, 500.0),
            "mealType" to if (mealType in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealType else "Lunch",
            "dateKey" to dateKey.take(20).ifBlank { todayDateKey() },
            "source" to if (source in listOf("AI Scan", "Database", "Manual", "Copied")) source else "Manual",
            "confidence" to confidence.coerceIn(0.0, 1.0),
            "notes" to notes.take(300),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "foodName" to foodName.take(120).ifBlank { "Food Item" },
            "quantity" to quantity.coerceIn(1.0, 5000.0),
            "calories" to calories.coerceIn(0, 5000),
            "protein" to protein.coerceIn(0.0, 500.0),
            "carbohydrates" to carbohydrates.coerceIn(0.0, 1000.0),
            "fat" to fat.coerceIn(0.0, 500.0),
            "fiber" to fiber.coerceIn(0.0, 200.0),
            "sugar" to sugar.coerceIn(0.0, 500.0),
            "mealType" to if (mealType in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealType else "Lunch",
            "dateKey" to dateKey.take(20).ifBlank { todayDateKey() },
            "source" to if (source in listOf("AI Scan", "Database", "Manual", "Copied")) source else "Manual",
            "confidence" to confidence.coerceIn(0.0, 1.0),
            "notes" to notes.take(300),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class WeightRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weight: Double = 65.0,
    val dateKey: String = todayDateKey(),
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "weight" to weight.coerceIn(15.0, 350.0),
        "dateKey" to dateKey.take(20).ifBlank { todayDateKey() },
        "note" to note.take(200),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class WaterRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quantity: Int = 250,
    val dateKey: String = todayDateKey(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "quantity" to quantity.coerceIn(1, 5000),
        "dateKey" to dateKey.take(20).ifBlank { todayDateKey() },
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "quantity" to quantity.coerceIn(1, 5000),
        "dateKey" to dateKey.take(20).ifBlank { todayDateKey() },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MealPlanEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val dietType: String = "Bengali Traditional",
    val targetCalories: Int = 1800,
    val breakfastSummary: String = "",
    val lunchSummary: String = "",
    val dinnerSummary: String = "",
    val snacksSummary: String = "",
    val coachTips: String = "",
    val dateKey: String = todayDateKey(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(150).ifBlank { "Balanced Daily Meal Plan" },
        "dietType" to dietType.take(60).ifBlank { "Balanced" },
        "targetCalories" to targetCalories.coerceIn(500, 6000),
        "breakfastSummary" to breakfastSummary.take(500).ifBlank { "Oats & Fruit Bowl" },
        "lunchSummary" to lunchSummary.take(500).ifBlank { "Rice, Dal & Sabzi" },
        "dinnerSummary" to dinnerSummary.take(500).ifBlank { "Roti & Tadka Dal" },
        "snacksSummary" to snacksSummary.take(500).ifBlank { "Roasted Chana & Green Tea" },
        "coachTips" to coachTips.take(600),
        "dateKey" to dateKey.take(20).ifBlank { todayDateKey() },
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AiCoachMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val question: String = "",
    val response: String = "",
    val language: String = "en",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "question" to question.take(1000).ifBlank { "Nutrition query" },
        "response" to response.take(4000).ifBlank { "Here is a balanced nutrition tip." },
        "language" to if (language in listOf("en", "bn", "hi")) language else "en",
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class ScannedFoodItem(
    val foodName: String,
    val quantityG: Double,
    val caloriesKcal: Int,
    val proteinG: Double,
    val carbohydratesG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double,
    val confidence: Double,
    val assumptions: String
)

data class TotalNutritionSummary(
    val totalWeightG: Double,
    val caloriesKcal: Int,
    val proteinG: Double,
    val carbohydratesG: Double,
    val fatG: Double,
    val fiberG: Double,
    val sugarG: Double
)

data class AiMealScanResponse(
    val status: String,
    val mealName: String,
    val foodItems: List<ScannedFoodItem>,
    val totalNutrition: TotalNutritionSummary,
    val analysisNotes: String,
    val requiresUserConfirmation: Boolean
) {
    fun scaledBy(multiplier: Float): AiMealScanResponse {
        val safeMultiplier = multiplier.coerceIn(0.25f, 3.0f).toDouble()
        val scaledItems = foodItems.map { item ->
            item.copy(
                quantityG = (((item.quantityG * safeMultiplier) * 10).roundToInt() / 10.0)
                    .coerceIn(1.0, 5000.0),
                caloriesKcal = (item.caloriesKcal * safeMultiplier).roundToInt()
                    .coerceIn(0, 5000),
                proteinG = (((item.proteinG * safeMultiplier) * 10).roundToInt() / 10.0)
                    .coerceIn(0.0, 500.0),
                carbohydratesG = (((item.carbohydratesG * safeMultiplier) * 10).roundToInt() / 10.0)
                    .coerceIn(0.0, 1000.0),
                fatG = (((item.fatG * safeMultiplier) * 10).roundToInt() / 10.0)
                    .coerceIn(0.0, 500.0),
                fiberG = (((item.fiberG * safeMultiplier) * 10).roundToInt() / 10.0)
                    .coerceIn(0.0, 200.0),
                sugarG = (((item.sugarG * safeMultiplier) * 10).roundToInt() / 10.0)
                    .coerceIn(0.0, 500.0)
            )
        }
        return copy(
            foodItems = scaledItems,
            totalNutrition = recalculateTotals(scaledItems)
        )
    }

    companion object {
        /**
         * Recalculates total nutrition strictly from the individual food items
         * so totals are always mathematically consistent after user edits or AI output.
         */
        fun recalculateTotals(items: List<ScannedFoodItem>): TotalNutritionSummary {
            return TotalNutritionSummary(
                totalWeightG = items.sumOf { it.quantityG },
                caloriesKcal = items.sumOf { it.caloriesKcal },
                proteinG = (items.sumOf { it.proteinG } * 10).roundToInt() / 10.0,
                carbohydratesG = (items.sumOf { it.carbohydratesG } * 10).roundToInt() / 10.0,
                fatG = (items.sumOf { it.fatG } * 10).roundToInt() / 10.0,
                fiberG = (items.sumOf { it.fiberG } * 10).roundToInt() / 10.0,
                sugarG = (items.sumOf { it.sugarG } * 10).roundToInt() / 10.0
            )
        }
    }
}

data class FoodDatabaseItem(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val nameHi: String,
    val category: String,
    val servingSizeG: Double,
    val servingLabel: String,
    val calories: Int,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val cookingMethod: String = "Standard preparation",
    val cuisine: String = "Indian",
    val prepTimeMinutes: Int = 20,
    val ingredients: List<String> = emptyList(),
    val recipeSteps: List<String> = emptyList()
) {
    val hasRecipe: Boolean
        get() = ingredients.isNotEmpty() && recipeSteps.isNotEmpty()

    fun localizedName(lang: String): String = when (lang) {
        "bn" -> "$nameBn ($nameEn)"
        "hi" -> "$nameHi ($nameEn)"
        else -> "$nameEn ($nameBn)"
    }
}

@IgnoreExtraProperties
data class SavedRecipeItem(
    val foodName: String = "",
    val quantityG: Double = 100.0,
    val caloriesKcal: Int = 0,
    val proteinG: Double = 0.0,
    val carbohydratesG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val mealSlot: String = "Lunch"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "foodName" to foodName.take(120).ifBlank { "Food Item" },
        "quantityG" to quantityG.coerceIn(1.0, 5000.0),
        "caloriesKcal" to caloriesKcal.coerceIn(0, 5000),
        "proteinG" to proteinG.coerceIn(0.0, 500.0),
        "carbohydratesG" to carbohydratesG.coerceIn(0.0, 1000.0),
        "fatG" to fatG.coerceIn(0.0, 500.0),
        "fiberG" to fiberG.coerceIn(0.0, 200.0),
        "mealSlot" to if (mealSlot in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealSlot else "Lunch"
    )
}

@IgnoreExtraProperties
data class SavedRecipe(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = CATEGORY_SCANNED_COMBO,
    val mealType: String = "Lunch",
    val totalCalories: Int = 0,
    val totalProteinG: Double = 0.0,
    val totalCarbsG: Double = 0.0,
    val totalFatG: Double = 0.0,
    val totalFiberG: Double = 0.0,
    val totalWeightG: Double = 0.0,
    val items: List<SavedRecipeItem> = emptyList(),
    val notes: String = "",
    @get:PropertyName("isFavorite")
    @field:PropertyName("isFavorite")
    val isFavorite: Boolean = true,
    val usageCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun withRecalculatedTotals(): SavedRecipe {
        val safeItems = items.take(30)
        return copy(
            items = safeItems,
            totalCalories = safeItems.sumOf { it.caloriesKcal }.coerceIn(0, 10000),
            totalProteinG = ((safeItems.sumOf { it.proteinG } * 10).roundToInt() / 10.0).coerceIn(0.0, 1000.0),
            totalCarbsG = ((safeItems.sumOf { it.carbohydratesG } * 10).roundToInt() / 10.0).coerceIn(0.0, 2000.0),
            totalFatG = ((safeItems.sumOf { it.fatG } * 10).roundToInt() / 10.0).coerceIn(0.0, 1000.0),
            totalFiberG = ((safeItems.sumOf { it.fiberG } * 10).roundToInt() / 10.0).coerceIn(0.0, 500.0),
            totalWeightG = ((safeItems.sumOf { it.quantityG } * 10).roundToInt() / 10.0).coerceIn(0.0, 15000.0)
        )
    }

    fun toFoodDiaryEntries(
        dateKey: String = todayDateKey(),
        targetMealTypeOverride: String? = null
    ): List<FoodDiaryEntry> {
        val validSlots = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val fallbackMeal = if (mealType in validSlots) mealType else "Lunch"
        val effectiveItems = items.ifEmpty {
            listOf(
                SavedRecipeItem(
                    foodName = title.ifBlank { "Saved Recipe" },
                    quantityG = totalWeightG.coerceAtLeast(100.0),
                    caloriesKcal = totalCalories,
                    proteinG = totalProteinG,
                    carbohydratesG = totalCarbsG,
                    fatG = totalFatG,
                    fiberG = totalFiberG,
                    mealSlot = fallbackMeal
                )
            )
        }
        return effectiveItems.map { item ->
            val resolvedSlot = when {
                targetMealTypeOverride != null && targetMealTypeOverride in validSlots -> targetMealTypeOverride
                mealType == "Full Day" && item.mealSlot in validSlots -> item.mealSlot
                item.mealSlot in validSlots && mealType == "Full Day" -> item.mealSlot
                else -> fallbackMeal
            }
            FoodDiaryEntry(
                userId = userId,
                foodName = item.foodName.ifBlank { title.ifBlank { "Saved Recipe" } },
                quantity = item.quantityG.coerceIn(1.0, 5000.0),
                calories = item.caloriesKcal.coerceIn(0, 5000),
                protein = item.proteinG.coerceIn(0.0, 500.0),
                carbohydrates = item.carbohydratesG.coerceIn(0.0, 1000.0),
                fat = item.fatG.coerceIn(0.0, 500.0),
                fiber = item.fiberG.coerceIn(0.0, 200.0),
                sugar = 0.0,
                mealType = resolvedSlot,
                dateKey = dateKey,
                source = "Manual",
                confidence = 1.0,
                notes = "From My Recipes: ${title.take(100)}"
            )
        }
    }

    fun toCreateMap(): Map<String, Any> {
        val normalizedItems = (if (items.isNotEmpty()) items else listOf(
            SavedRecipeItem(
                foodName = title.take(120).ifBlank { "Custom Recipe" },
                quantityG = totalWeightG.coerceIn(1.0, 5000.0),
                caloriesKcal = totalCalories.coerceIn(0, 5000),
                proteinG = totalProteinG.coerceIn(0.0, 500.0),
                carbohydratesG = totalCarbsG.coerceIn(0.0, 1000.0),
                fatG = totalFatG.coerceIn(0.0, 500.0),
                fiberG = totalFiberG.coerceIn(0.0, 200.0),
                mealSlot = if (mealType in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealType else "Lunch"
            )
        )).take(30)

        return mapOf(
            "userId" to userId,
            "title" to title.take(140).ifBlank { "Saved Meal" },
            "category" to if (category in ALLOWED_CATEGORIES) category else CATEGORY_SCANNED_COMBO,
            "mealType" to if (mealType in ALLOWED_MEAL_TYPES) mealType else "Lunch",
            "totalCalories" to totalCalories.coerceIn(0, 10000),
            "totalProteinG" to totalProteinG.coerceIn(0.0, 1000.0),
            "totalCarbsG" to totalCarbsG.coerceIn(0.0, 2000.0),
            "totalFatG" to totalFatG.coerceIn(0.0, 1000.0),
            "totalFiberG" to totalFiberG.coerceIn(0.0, 500.0),
            "totalWeightG" to totalWeightG.coerceIn(0.0, 15000.0),
            "items" to normalizedItems.map { it.toMap() },
            "notes" to notes.take(500),
            "isFavorite" to isFavorite,
            "usageCount" to usageCount.coerceIn(0, 100000),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        val normalizedItems = (if (items.isNotEmpty()) items else listOf(
            SavedRecipeItem(
                foodName = title.take(120).ifBlank { "Custom Recipe" },
                quantityG = totalWeightG.coerceIn(1.0, 5000.0),
                caloriesKcal = totalCalories.coerceIn(0, 5000),
                proteinG = totalProteinG.coerceIn(0.0, 500.0),
                carbohydratesG = totalCarbsG.coerceIn(0.0, 1000.0),
                fatG = totalFatG.coerceIn(0.0, 500.0),
                fiberG = totalFiberG.coerceIn(0.0, 200.0),
                mealSlot = if (mealType in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealType else "Lunch"
            )
        )).take(30)

        return mapOf(
            "title" to title.take(140).ifBlank { "Saved Meal" },
            "category" to if (category in ALLOWED_CATEGORIES) category else CATEGORY_SCANNED_COMBO,
            "mealType" to if (mealType in ALLOWED_MEAL_TYPES) mealType else "Lunch",
            "totalCalories" to totalCalories.coerceIn(0, 10000),
            "totalProteinG" to totalProteinG.coerceIn(0.0, 1000.0),
            "totalCarbsG" to totalCarbsG.coerceIn(0.0, 2000.0),
            "totalFatG" to totalFatG.coerceIn(0.0, 1000.0),
            "totalFiberG" to totalFiberG.coerceIn(0.0, 500.0),
            "totalWeightG" to totalWeightG.coerceIn(0.0, 15000.0),
            "items" to normalizedItems.map { it.toMap() },
            "notes" to notes.take(500),
            "isFavorite" to isFavorite,
            "usageCount" to usageCount.coerceIn(0, 100000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        const val CATEGORY_SCANNED_COMBO = "Scanned Combo"
        const val CATEGORY_CUSTOM_MEAL_PLAN = "Custom Meal Plan"
        const val CATEGORY_FAVORITE_MEAL = "Favorite Meal"

        val ALLOWED_CATEGORIES = listOf(
            CATEGORY_SCANNED_COMBO,
            CATEGORY_CUSTOM_MEAL_PLAN,
            CATEGORY_FAVORITE_MEAL
        )

        val ALLOWED_MEAL_TYPES = listOf(
            "Breakfast",
            "Lunch",
            "Dinner",
            "Snacks",
            "Full Day"
        )

        fun fromScannedMeal(
            userId: String,
            customTitle: String,
            scan: AiMealScanResponse,
            mealType: String,
            notes: String = ""
        ): SavedRecipe {
            val recipeItems = scan.foodItems.map { item ->
                SavedRecipeItem(
                    foodName = item.foodName,
                    quantityG = item.quantityG,
                    caloriesKcal = item.caloriesKcal,
                    proteinG = item.proteinG,
                    carbohydratesG = item.carbohydratesG,
                    fatG = item.fatG,
                    fiberG = item.fiberG,
                    mealSlot = if (mealType in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) mealType else "Lunch"
                )
            }
            return SavedRecipe(
                userId = userId,
                title = customTitle.ifBlank { scan.mealName }.ifBlank { "Scanned Combo" },
                category = CATEGORY_SCANNED_COMBO,
                mealType = if (mealType in ALLOWED_MEAL_TYPES) mealType else "Lunch",
                items = recipeItems,
                notes = notes.ifBlank { scan.analysisNotes },
                isFavorite = true
            ).withRecalculatedTotals()
        }

        fun fromDiaryEntries(
            userId: String,
            title: String,
            mealType: String,
            entries: List<FoodDiaryEntry>,
            category: String = CATEGORY_FAVORITE_MEAL,
            notes: String = ""
        ): SavedRecipe {
            val recipeItems = entries.map { entry ->
                SavedRecipeItem(
                    foodName = entry.foodName,
                    quantityG = entry.quantity,
                    caloriesKcal = entry.calories,
                    proteinG = entry.protein,
                    carbohydratesG = entry.carbohydrates,
                    fatG = entry.fat,
                    fiberG = entry.fiber,
                    mealSlot = if (entry.mealType in listOf("Breakfast", "Lunch", "Dinner", "Snacks")) entry.mealType else "Lunch"
                )
            }
            return SavedRecipe(
                userId = userId,
                title = title.ifBlank { "$mealType Combo" },
                category = category,
                mealType = if (mealType in ALLOWED_MEAL_TYPES) mealType else "Lunch",
                items = recipeItems,
                notes = notes,
                isFavorite = true
            ).withRecalculatedTotals()
        }

        fun fromMealPlan(
            userId: String,
            plan: MealPlanEntry
        ): SavedRecipe {
            val bCal = (plan.targetCalories * 0.25).roundToInt()
            val lCal = (plan.targetCalories * 0.35).roundToInt()
            val dCal = (plan.targetCalories * 0.28).roundToInt()
            val sCal = (plan.targetCalories - bCal - lCal - dCal).coerceAtLeast(80)

            fun buildMealItem(slot: String, summary: String, kcal: Int): SavedRecipeItem {
                val protein = ((kcal * 0.22) / 4.0 * 10).roundToInt() / 10.0
                val carbs = ((kcal * 0.50) / 4.0 * 10).roundToInt() / 10.0
                val fat = ((kcal * 0.28) / 9.0 * 10).roundToInt() / 10.0
                return SavedRecipeItem(
                    foodName = summary.ifBlank { "$slot Meal" }.take(120),
                    quantityG = 250.0,
                    caloriesKcal = kcal,
                    proteinG = protein,
                    carbohydratesG = carbs,
                    fatG = fat,
                    fiberG = 5.0,
                    mealSlot = slot
                )
            }

            val planItems = listOf(
                buildMealItem("Breakfast", plan.breakfastSummary, bCal),
                buildMealItem("Lunch", plan.lunchSummary, lCal),
                buildMealItem("Dinner", plan.dinnerSummary, dCal),
                buildMealItem("Snacks", plan.snacksSummary, sCal)
            )
            return SavedRecipe(
                userId = userId,
                title = plan.title.ifBlank { "${plan.dietType} Meal Plan" },
                category = CATEGORY_CUSTOM_MEAL_PLAN,
                mealType = "Full Day",
                items = planItems,
                notes = plan.coachTips.ifBlank { "${plan.dietType} • ${plan.targetCalories} kcal daily plan" },
                isFavorite = true
            ).withRecalculatedTotals()
        }
    }
}

