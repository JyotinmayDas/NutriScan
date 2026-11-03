package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
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
    val dailyWaterGoalMl: Int = 2500,
    val language: String = "en",
    val isPremium: Boolean = false,
    val dailyScansUsed: Int = 0,
    val lastScanDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
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
    val cookingMethod: String = "Standard preparation"
) {
    fun localizedName(lang: String): String = when (lang) {
        "bn" -> "$nameBn ($nameEn)"
        "hi" -> "$nameHi ($nameEn)"
        else -> "$nameEn ($nameBn)"
    }
}
